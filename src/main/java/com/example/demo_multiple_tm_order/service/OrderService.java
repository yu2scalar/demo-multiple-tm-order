package com.example.demo_multiple_tm_order.service;

import com.example.demo_multiple_tm_order.model.Order;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.dto.SqlCommandDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.example.demo_multiple_tm_order.mapper.OrderMapper;
import com.example.demo_multiple_tm_order.repository.OrderRepository;
import com.example.demo_multiple_tm_order.util.ExecuteSqlUtil;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.io.Key;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.sql.ResultSet;
import com.scalar.db.sql.SqlSession;
import com.scalar.db.sql.SqlSessionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderService {
    DistributedTransactionManager manager;
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    OrderRepository orderRepository;

    public OrderService(DistributedTransactionManager manager, SqlSessionFactory sqlSessionFactory) throws InstantiationException, IllegalAccessException {
        this.manager = manager;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // Execute SQL Command
    public List<OrderDto> executeSQL(SqlCommandDto sqlCommandDto) throws CustomException {
        SqlSession sqlSession = null;

        try {
            sqlSession = sqlSessionFactory.createSqlSession();
            String sqlCommand = sqlCommandDto.getSqlCommand();

            // Begin a transaction
            sqlSession.begin();

            List<OrderDto> result;
            if (isDmlOperation(sqlCommand)) {
                // Handle DML operations (INSERT, UPDATE, DELETE)
                ResultSet resultSet = sqlSession.execute(sqlCommand);
                // For DML operations, return empty list but operation was successful
                result = new ArrayList<>();
            } else {
                // Handle SELECT operations
                ExecuteSqlUtil<Order> executeSql = new ExecuteSqlUtil<>(Order.class);
                List<Order> orderList = executeSql.executeSQL(sqlSession, sqlCommand);
                result = OrderMapper.mapToOrderDtoList(orderList);
            }

            sqlSession.commit();
            return result;
        } catch (Exception e) {
            handleSqlSessionException(e, sqlSession);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Helper method to detect DML operations
    private boolean isDmlOperation(String sqlCommand) {
        String trimmedCommand = sqlCommand.trim().toUpperCase();
        return trimmedCommand.startsWith("INSERT") || 
               trimmedCommand.startsWith("UPDATE") || 
               trimmedCommand.startsWith("DELETE");
    }

    // Create Record
    public ResponseStatusDto insertOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.start();
            order = orderRepository.insertOrder(transaction, order);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.start();
            order = orderRepository.upsertOrder(transaction, order);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public OrderDto getOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.start();
            order = orderRepository.getOrder(transaction, order);
            transaction.commit();
            return OrderMapper.mapToOrderDto(order);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.start();
            order = orderRepository.updateOrder(transaction, order);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteOrder(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.start();
            orderRepository.deleteOrder(transaction, order);
            transaction.commit();
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<OrderDto> getOrderListAll() throws CustomException {
        DistributedTransaction transaction = null;
        List<Order> orderList = new ArrayList<>();
        try {
            transaction = manager.start();
            orderList = orderRepository.getOrderListAll(transaction);
            transaction.commit();
            return OrderMapper.mapToOrderDtoList(orderList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<OrderDto> getOrderListByPk(OrderDto orderDto) throws CustomException {
        DistributedTransaction transaction = null;
        List<Order> orderList = new ArrayList<>();
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            Key partitionKey = order.getPartitionKey();
            transaction = manager.start();
            orderList = orderRepository.getOrderListByPk(transaction, partitionKey);
            transaction.commit();
            return OrderMapper.mapToOrderDtoList(orderList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    private void handleTransactionException(Exception e, DistributedTransaction transaction) {
        log.error(e.getMessage(), e);
        if (transaction != null) {
            try {
                transaction.rollback();
            } catch (RollbackException ex) {
                log.error(ex.getMessage(), ex);
            }
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