package com.example.demo_multiple_tm_order.service;

import com.example.demo_multiple_tm_order.dto.SqlCommandDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.example.demo_multiple_tm_order.util.GenericSqlUtil;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.sql.SqlSession;
import com.scalar.db.sql.SqlSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GenericSqlService {
    SqlSessionFactory sqlSessionFactory;

    public GenericSqlService(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // Execute SQL Command (Generic - returns Map)
    public List<Map<String, Object>> executeSQLGeneric(SqlCommandDto sqlCommandDto) throws CustomException {
        SqlSession sqlSession = null;

        try {
            sqlSession = sqlSessionFactory.createSqlSession();
            GenericSqlUtil genericSqlUtil = new GenericSqlUtil(sqlSession);

            // Begin a transaction
            sqlSession.begin();

            List<Map<String, Object>> resultList = genericSqlUtil.executeQuery(sqlCommandDto.getSqlCommand());

            sqlSession.commit();
            return resultList;
        } catch (Exception e) {
            handleSqlSessionException(e, sqlSession);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    private void handleSqlSessionException(Exception e, SqlSession sqlSession) {
        log.error(e.getMessage(), e);
        if (sqlSession != null) {
            try {
                sqlSession.rollback();
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }

    private int determineErrorCode(Exception e) {
        if (e instanceof UnsatisfiedConditionException) return 9100;
        if (e instanceof UnknownTransactionStatusException) return 9200;
        if (e instanceof TransactionException) return 9300;
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}
