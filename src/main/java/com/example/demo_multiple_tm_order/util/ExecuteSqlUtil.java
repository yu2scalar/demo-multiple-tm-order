package com.example.demo_multiple_tm_order.util;

import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.sql.Record;
import com.scalar.db.sql.*;
import org.apache.commons.text.CaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for executing SQL queries and mapping results to entity objects.
 * This class uses reflection to map ScalarDB records to Java objects with validation.
 *
 * @param <T> The type of entity object to map results to
 */
public class ExecuteSqlUtil<T> {
    private static final Logger logger = LoggerFactory.getLogger(ExecuteSqlUtil.class);
    
    // Cache for reflection metadata to improve performance
    private static final Map<Class<?>, Map<String, FieldSetterPair>> fieldCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Set<String>> entityColumnsCache = new ConcurrentHashMap<>();
    
    // SQL injection protection patterns
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(/\\*.*?\\*/|--.*?$|#.*?$)", Pattern.MULTILINE | Pattern.DOTALL);
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
        "\\b(ALTER|CREATE|DROP|EXEC(UTE)?|MERGE|SELECT\\s+\\*\\s+FROM\\s+INFORMATION_SCHEMA|" +
        "GRANT|REVOKE|SHUTDOWN|TRUNCATE)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MULTIPLE_QUERIES = Pattern.compile(";\\s*(?=(?:[^']*'[^']*')*[^']*$)");
    private static final Pattern SQL_INJECTION_PATTERNS = Pattern.compile(
        "('\\s*(OR|AND)\\s*'?\\d*'?\\s*=\\s*'?\\d*'?)|" +  // 'OR'1'='1'
        "(\\bOR\\b\\s+\\d+\\s*=\\s*\\d+)|" +                // OR 1=1
        "(\\bUNION\\b.*\\bSELECT\\b)|" +                    // UNION SELECT
        "(\\bDROP\\b.*\\b(TABLE|DATABASE)\\b)|" +           // DROP TABLE/DATABASE
        "(;\\s*--)|" +                                       // ; --
        "(\\bEXEC(UTE)?\\b\\s+\\w+)|" +                     // EXEC/EXECUTE stored procedures
        "(\\bxp_\\w+)|" +                                   // xp_ procedures
        "(\\bsp_\\w+)",                                     // sp_ procedures
        Pattern.CASE_INSENSITIVE
    );
    
    private final Class<T> entityClass;
    private final Constructor<T> constructor;
    private final Map<String, FieldSetterPair> fieldSetterMap;
    private final Set<String> entityColumns;
    
    /**
     * Creates a new ExecuteSqlUtil instance for the specified entity class.
     *
     * @param entityClass The class of entities to create from query results
     * @throws IllegalArgumentException if the entity class cannot be instantiated
     */
    public ExecuteSqlUtil(Class<T> entityClass) {
        this.entityClass = entityClass;
        try {
            this.constructor = entityClass.getDeclaredConstructor();
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                "Entity class " + entityClass.getName() + " must have a no-argument constructor", e);
        }
        
        // Initialize or retrieve cached field mappings
        this.fieldSetterMap = fieldCache.computeIfAbsent(entityClass, this::buildFieldSetterMap);
        this.entityColumns = entityColumnsCache.computeIfAbsent(entityClass, this::extractEntityColumns);
    }
    
    /**
     * Executes a SQL query and maps the results to a list of entity objects.
     *
     * @param sqlSession The SQL session to execute the query
     * @param statement The SQL statement to execute
     * @return List of mapped entity objects
     * @throws CrudException if the query execution fails
     * @throws ValidationException if validation fails
     */
    public List<T> executeSQL(SqlSession sqlSession, String statement) throws CrudException {
        // Validate SQL statement
        validateSqlStatement(statement);
        
        try {
            List<T> results = new ArrayList<>();
            
            ResultSet resultSet = sqlSession.execute(statement);
            List<Record> records = resultSet.all();
            ColumnDefinitions columnDefinitions = resultSet.getColumnDefinitions();
            
            // Validate that all result columns can be mapped to entity fields
            validateResultColumns(columnDefinitions);
            
            for (Record record : records) {
                results.add(mapRecordToEntity(record, columnDefinitions));
            }
            
            return results;
        } catch (ValidationException e) {
            throw e; // Re-throw validation exceptions
        } catch (Exception e) {
            if (e instanceof CrudException) {
                throw (CrudException) e;
            }
            throw new IllegalStateException("Failed to execute SQL and map results", e);
        }
    }
    
    /**
     * Validates the SQL statement for potential security issues.
     *
     * @param statement The SQL statement to validate
     * @throws ValidationException if potential SQL injection is detected
     */
    private void validateSqlStatement(String statement) {
        if (statement == null || statement.trim().isEmpty()) {
            throw new ValidationException("SQL statement cannot be null or empty");
        }
        
        // Remove comments for analysis
        String cleanedStatement = COMMENT_PATTERN.matcher(statement).replaceAll("");
        
        // Check for multiple queries
        if (MULTIPLE_QUERIES.matcher(cleanedStatement).find()) {
            throw new ValidationException("Multiple queries are not allowed");
        }
        
        // Check for dangerous DDL/admin keywords
        Matcher dangerousMatcher = DANGEROUS_KEYWORDS.matcher(cleanedStatement);
        if (dangerousMatcher.find()) {
            throw new ValidationException("Dangerous SQL keyword detected: " + dangerousMatcher.group());
        }
        
        // Check for common SQL injection patterns
        if (SQL_INJECTION_PATTERNS.matcher(cleanedStatement).find()) {
            throw new ValidationException("Potential SQL injection pattern detected");
        }
        
        // Additional validation for DML statements
        String upperStatement = cleanedStatement.trim().toUpperCase();
        if (upperStatement.startsWith("DELETE") || upperStatement.startsWith("UPDATE") || upperStatement.startsWith("INSERT")) {
            // Check for suspicious patterns in DML statements
            // Allow simple WHERE clauses but block complex injection attempts
            if (cleanedStatement.matches("(?i).*\\b(OR|AND)\\s+['\"]?\\w*['\"]?\\s*=\\s*['\"]?\\w*['\"]?.*")) {
                // Check if it's a suspicious always-true condition like OR 1=1 or OR 'a'='a'
                if (cleanedStatement.matches("(?i).*\\b(OR|AND)\\s+(['\"]?)(\\w+)\\2\\s*=\\s*\\2\\3\\2.*")) {
                    throw new ValidationException("Suspicious WHERE clause detected");
                }
            }
        }
    }
    
    /**
     * Validates that all columns in the result set can be mapped to entity fields.
     *
     * @param columnDefinitions The column definitions from the result set
     * @throws ValidationException if columns cannot be mapped to entity
     */
    private void validateResultColumns(ColumnDefinitions columnDefinitions) {
        List<String> unmappedColumns = new ArrayList<>();
        
        for (ColumnDefinition column : columnDefinitions) {
            String columnName = column.getColumnName();
            String camelCaseName = CaseUtils.toCamelCase(columnName, false, '_');
            
            // Check if this column can be mapped to an entity field
            if (!fieldSetterMap.containsKey(camelCaseName)) {
                unmappedColumns.add(columnName);
            }
        }
        
        if (!unmappedColumns.isEmpty()) {
            String entityName = entityClass.getSimpleName();
            String availableFields = fieldSetterMap.keySet().stream()
                .sorted()
                .collect(Collectors.joining(", "));
            
            throw new ValidationException(
                String.format("Cannot map columns %s to entity %s. Available fields: [%s]",
                    unmappedColumns, entityName, availableFields)
            );
        }
    }
    
    /**
     * Validates that requested columns exist in the entity model.
     *
     * @param requestedColumns Set of column names requested in the query
     * @throws ValidationException if columns don't exist in entity
     */
    public void validateRequestedColumns(Set<String> requestedColumns) {
        Set<String> invalidColumns = new HashSet<>();
        
        for (String column : requestedColumns) {
            String normalizedColumn = column.toLowerCase().replace("_", "");
            boolean found = false;
            
            // Check against entity's static column constants and field names
            for (String entityColumn : entityColumns) {
                if (entityColumn.toLowerCase().replace("_", "").equals(normalizedColumn)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                invalidColumns.add(column);
            }
        }
        
        if (!invalidColumns.isEmpty()) {
            String availableColumns = entityColumns.stream()
                .sorted()
                .collect(Collectors.joining(", "));
            
            throw new ValidationException(
                String.format("Columns %s are not defined in entity %s. Available columns: [%s]",
                    invalidColumns, entityClass.getSimpleName(), availableColumns)
            );
        }
    }
    
    /**
     * Sanitizes a value for safe inclusion in SQL queries.
     *
     * @param value The value to sanitize
     * @return The sanitized value
     */
    public static String sanitizeValue(String value) {
        if (value == null) {
            return null;
        }
        
        // Escape single quotes
        String sanitized = value.replace("'", "''");
        
        // Remove or escape other potentially dangerous characters
        sanitized = sanitized.replaceAll("[\\x00-\\x1f\\x7f-\\x9f]", ""); // Remove control characters
        
        // Check for SQL injection patterns in the value
        if (SQL_INJECTION_PATTERNS.matcher(sanitized).find()) {
            throw new ValidationException("Potentially dangerous value detected");
        }
        
        return sanitized;
    }
    
    /**
     * Maps a single record to an entity object.
     *
     * @param record The record to map
     * @param columnDefinitions The column definitions from the result set
     * @return The mapped entity object
     */
    private T mapRecordToEntity(Record record, ColumnDefinitions columnDefinitions) {
        try {
            T entity = constructor.newInstance();
            
            for (ColumnDefinition column : columnDefinitions) {
                String columnName = column.getColumnName();
                String camelCaseName = CaseUtils.toCamelCase(columnName, false, '_');
                
                FieldSetterPair fieldSetter = fieldSetterMap.get(camelCaseName);
                if (fieldSetter != null) {
                    setFieldValue(entity, fieldSetter, record, column);
                }
                // Note: validation already ensured all columns can be mapped
            }
            
            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to map record to entity", e);
        }
    }
    
    /**
     * Sets a field value on the entity based on the record data.
     */
    private void setFieldValue(T entity, FieldSetterPair fieldSetter, Record record, ColumnDefinition column) 
            throws Exception {
        String columnName = column.getColumnName();
        Class<?> fieldType = fieldSetter.field.getType();
        
        // Handle null values
        if (record.isNull(columnName)) {
            if (!fieldType.isPrimitive()) {
                fieldSetter.setter.invoke(entity, (Object) null);
            } else {
                // Set default values for primitive types
                fieldSetter.setter.invoke(entity, getDefaultPrimitiveValue(fieldType));
            }
            return;
        }
        
        // Map non-null values based on data type
        Object value = extractValue(record, column, fieldType);
        fieldSetter.setter.invoke(entity, value);
    }
    
    /**
     * Extracts a value from the record based on the column data type.
     */
    private Object extractValue(Record record, ColumnDefinition column, Class<?> fieldType) {
        String columnName = column.getColumnName();
        DataType dataType = column.getDataType();
        
        switch (dataType) {
            case BOOLEAN:
                return record.getBoolean(columnName);
            case INT:
                return record.getInt(columnName);
            case BIGINT:
                return record.getBigInt(columnName);
            case FLOAT:
                return record.getFloat(columnName);
            case DOUBLE:
                return record.getDouble(columnName);
            case TEXT:
                return record.getText(columnName);
            case BLOB:
                return record.getBlobAsBytes(columnName);
            case DATE:
                return record.getDate(columnName);
            case TIME:
                return record.getTime(columnName);
            case TIMESTAMP:
                return record.getTimestamp(columnName);
            case TIMESTAMPTZ:
                return record.getTimestampTZ(columnName);
            default:
                logger.warn("Unsupported data type {} for column {}", dataType, columnName);
                return null;
        }
    }
    
    /**
     * Returns the default value for a primitive type.
     */
    private Object getDefaultPrimitiveValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return '\0';
        throw new IllegalArgumentException("Unknown primitive type: " + type);
    }
    
    /**
     * Builds a map of field names to field/setter pairs for efficient lookup.
     */
    private Map<String, FieldSetterPair> buildFieldSetterMap(Class<?> clazz) {
        Map<String, FieldSetterPair> map = new HashMap<>();
        
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }
            
            String fieldName = field.getName();
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            
            try {
                Method setter = clazz.getMethod(setterName, field.getType());
                field.setAccessible(true);
                map.put(fieldName, new FieldSetterPair(field, setter));
            } catch (NoSuchMethodException e) {
                logger.debug("No setter found for field: {} in class: {}", fieldName, clazz.getName());
            }
        }
        
        return map;
    }
    
    /**
     * Extracts all column names from the entity class (from static constants and field names).
     */
    private Set<String> extractEntityColumns(Class<?> clazz) {
        Set<String> columns = new HashSet<>();
        
        // Extract from static final String fields (column constants)
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) 
                    && field.getType() == String.class) {
                try {
                    field.setAccessible(true);
                    String value = (String) field.get(null);
                    if (value != null && !value.isEmpty()) {
                        columns.add(value);
                    }
                } catch (IllegalAccessException e) {
                    logger.debug("Cannot access field: {}", field.getName());
                }
            }
        }
        
        // Also add the actual field names (in snake_case format)
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
                // Convert camelCase to snake_case
                String snakeCase = field.getName()
                    .replaceAll("([a-z])([A-Z]+)", "$1_$2")
                    .toLowerCase();
                columns.add(snakeCase);
            }
        }
        
        return columns;
    }
    
    /**
     * Helper class to store field and setter method pairs.
     */
    private static class FieldSetterPair {
        final Field field;
        final Method setter;
        
        FieldSetterPair(Field field, Method setter) {
            this.field = field;
            this.setter = setter;
        }
    }
    
    /**
     * Exception thrown when validation fails.
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}