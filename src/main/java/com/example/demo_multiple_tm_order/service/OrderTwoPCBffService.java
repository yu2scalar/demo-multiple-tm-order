package com.example.demo_multiple_tm_order.service;

import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.scalar.db.api.TwoPhaseCommitTransaction;
import com.scalar.db.api.TwoPhaseCommitTransactionManager;
import com.scalar.db.exception.transaction.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Two-Phase Commit BFF Service for Order
 *
 * This service coordinates distributed transactions across multiple 2PC microservices by:
 * - Starting a ScalarDB 2PC transaction locally
 * - Propagating the transaction ID to 2PC services via ScalarDB-Transaction-ID HTTP header
 * - Calling 2PC REST endpoints for CRUD operations
 * - Orchestrating the 2PC protocol (prepare, validate, commit/rollback)
 *
 * Key concepts:
 * - Transaction ID propagation: 2PC services join the same transaction using the ID from headers
 * - Atomic operations: All services succeed together or all fail together
 * - 2PC protocol: prepare → validate → commit (or rollback on error)
 * - ApiResponse handling: 2PC services return ApiResponse<T> for consistent response structure
 */
@Slf4j
@Service
public class OrderTwoPCBffService extends BaseTwoPCBffService {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE_URL = "http://localhost:";

    public OrderTwoPCBffService(TwoPhaseCommitTransactionManager manager) {
        super(manager);
    }

    // Create Record
    public ResponseStatusDto insertOrder(OrderDto orderDto) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String baseUrl = BASE_URL + serverPort + "/order-two-pc";

            // Execute 2PC protocol
            executePost(baseUrl, orderDto, headers);
            executeTwoPcPhase(baseUrl + "/prepare", headers);
            executeTwoPcPhase(baseUrl + "/validate", headers);
            executeTwoPcPhase(baseUrl + "/commit", headers);
            log.info("2PC transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null) {
                String rollbackUrl = BASE_URL + serverPort + "/order-two-pc/rollback";
                handleTransactionRollback(transaction, rollbackUrl, transaction.getId());
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Upsert Record
    public ResponseStatusDto upsertOrder(OrderDto orderDto) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String baseUrl = BASE_URL + serverPort + "/order-two-pc";

            // Execute 2PC protocol
            executePost(baseUrl + "/upsert", orderDto, headers);
            executeTwoPcPhase(baseUrl + "/prepare", headers);
            executeTwoPcPhase(baseUrl + "/validate", headers);
            executeTwoPcPhase(baseUrl + "/commit", headers);
            log.info("2PC transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null) {
                String rollbackUrl = BASE_URL + serverPort + "/order-two-pc/rollback";
                handleTransactionRollback(transaction, rollbackUrl, transaction.getId());
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Record
    public OrderDto getOrder(OrderDto orderDto) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String baseUrl = BASE_URL + serverPort + "/order-two-pc";

            // Execute 2PC protocol
            OrderDto result = executeGet(
                baseUrl + "/" + orderDto.getId(),
                headers,
                new ParameterizedTypeReference<ApiResponse<OrderDto>>() {}
            );
            executeTwoPcPhase(baseUrl + "/prepare", headers);
            executeTwoPcPhase(baseUrl + "/validate", headers);
            executeTwoPcPhase(baseUrl + "/commit", headers);
            log.info("2PC transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null) {
                String rollbackUrl = BASE_URL + serverPort + "/order-two-pc/rollback";
                handleTransactionRollback(transaction, rollbackUrl, transaction.getId());
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Update Record
    public ResponseStatusDto updateOrder(OrderDto orderDto) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String baseUrl = BASE_URL + serverPort + "/order-two-pc";

            // Execute 2PC protocol
            executePut(baseUrl, orderDto, headers);
            executeTwoPcPhase(baseUrl + "/prepare", headers);
            executeTwoPcPhase(baseUrl + "/validate", headers);
            executeTwoPcPhase(baseUrl + "/commit", headers);
            log.info("2PC transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null) {
                String rollbackUrl = BASE_URL + serverPort + "/order-two-pc/rollback";
                handleTransactionRollback(transaction, rollbackUrl, transaction.getId());
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Delete Record
    public ResponseStatusDto deleteOrder(OrderDto orderDto) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String baseUrl = BASE_URL + serverPort + "/order-two-pc";

            // Execute 2PC protocol
            executeDelete(baseUrl + "/" + orderDto.getId(), headers);
            executeTwoPcPhase(baseUrl + "/prepare", headers);
            executeTwoPcPhase(baseUrl + "/validate", headers);
            executeTwoPcPhase(baseUrl + "/commit", headers);
            log.info("2PC transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null) {
                String rollbackUrl = BASE_URL + serverPort + "/order-two-pc/rollback";
                handleTransactionRollback(transaction, rollbackUrl, transaction.getId());
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve All Records
    public List<OrderDto> getOrderListAll() throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String baseUrl = BASE_URL + serverPort + "/order-two-pc";

            // Execute 2PC protocol
            List<OrderDto> result = executeGet(
                baseUrl + "/scan-all",
                headers,
                new ParameterizedTypeReference<ApiResponse<List<OrderDto>>>() {}
            );
            executeTwoPcPhase(baseUrl + "/prepare", headers);
            executeTwoPcPhase(baseUrl + "/validate", headers);
            executeTwoPcPhase(baseUrl + "/commit", headers);
            log.info("2PC transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null) {
                String rollbackUrl = BASE_URL + serverPort + "/order-two-pc/rollback";
                handleTransactionRollback(transaction, rollbackUrl, transaction.getId());
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }

    // Retrieve Records by Partition Key
    public List<OrderDto> getOrderListByPk(OrderDto orderDto) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);

            String baseUrl = BASE_URL + serverPort + "/order-two-pc";

            // Execute 2PC protocol
            List<OrderDto> result = executeGet(
                baseUrl + "/scan-by-pk" + "/" + orderDto.getId(),
                headers,
                new ParameterizedTypeReference<ApiResponse<List<OrderDto>>>() {}
            );
            executeTwoPcPhase(baseUrl + "/prepare", headers);
            executeTwoPcPhase(baseUrl + "/validate", headers);
            executeTwoPcPhase(baseUrl + "/commit", headers);
            log.info("2PC transaction committed: {}", transactionId);

            return result;
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null) {
                String rollbackUrl = BASE_URL + serverPort + "/order-two-pc/rollback";
                handleTransactionRollback(transaction, rollbackUrl, transaction.getId());
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }
}
