package com.example.demo_multiple_tm_order.service;

import com.example.demo_multiple_tm_order.model.Order;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.example.demo_multiple_tm_order.mapper.OrderMapper;
import com.example.demo_multiple_tm_order.repository.OrderTwoPCRepository;
import com.scalar.db.api.TwoPhaseCommitTransaction;
import com.scalar.db.api.TwoPhaseCommitTransactionManager;
import com.scalar.db.io.Key;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Two-Phase Commit (2PC) Service for Order
 *
 * This service extends Base2PCService and provides CRUD operations.
 * It joins an existing transaction using the provided transaction ID.
 *
 * Key characteristics:
 * - All CRUD methods accept DTO and transaction ID parameters
 * - Converts DTO to Model before calling Repository
 * - Uses manager.join(transactionId) for CRUD operations
 * - Does NOT commit in CRUD methods (BFF orchestrates commit via REST)
 * - Inherits lifecycle methods (prepare, validate, commit, rollback) from Base2PCService
 */
@Slf4j
@Service
public class OrderTwoPCService extends BaseTwoPCService {

    @Autowired
    OrderTwoPCRepository orderRepository;

    public OrderTwoPCService(TwoPhaseCommitTransactionManager manager) {
        super(manager);
    }

    // Create Record
    public ResponseStatusDto insertOrder(OrderDto orderDto, String transactionId) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.join(transactionId);
            order = orderRepository.insertOrder(transaction, order);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertOrder(OrderDto orderDto, String transactionId) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.join(transactionId);
            order = orderRepository.upsertOrder(transaction, order);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public OrderDto getOrder(OrderDto orderDto, String transactionId) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.join(transactionId);
            order = orderRepository.getOrder(transaction, order);
            return OrderMapper.mapToOrderDto(order);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateOrder(OrderDto orderDto, String transactionId) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.join(transactionId);
            order = orderRepository.updateOrder(transaction, order);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteOrder(OrderDto orderDto, String transactionId) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            transaction = manager.join(transactionId);
            orderRepository.deleteOrder(transaction, order);
            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<OrderDto> getOrderListAll(String transactionId) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        List<Order> orderList = new ArrayList<>();
        try {
            transaction = manager.join(transactionId);
            orderList = orderRepository.getOrderListAll(transaction);
            return OrderMapper.mapToOrderDtoList(orderList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<OrderDto> getOrderListByPk(OrderDto orderDto, String transactionId) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        List<Order> orderList = new ArrayList<>();
        try {
            Order order = OrderMapper.mapToOrder(orderDto);
            Key partitionKey = order.getPartitionKey();
            transaction = manager.join(transactionId);
            orderList = orderRepository.getOrderListByPk(transaction, partitionKey);
            return OrderMapper.mapToOrderDtoList(orderList);
        } catch (Exception e) {
            handleTransactionException(e, transaction);
            throw new CustomException(e, determineErrorCode(e));
        }
    }
}
