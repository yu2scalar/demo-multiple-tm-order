package com.example.demo_multiple_tm_order.service;

import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ProductDto;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.scalar.db.api.TwoPhaseCommitTransaction;
import com.scalar.db.api.TwoPhaseCommitTransactionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

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
public class PlaceOrderTwoPCBffService extends BaseTwoPCBffService {

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE_URL4ORDER = "http://localhost:8080";
    private static final String BASE_URL4INVENTORY = "http://localhost:8081";
    String urlInventory = BASE_URL4INVENTORY + "/product-two-pc";
    String urlOrder = BASE_URL4ORDER + "/order-two-pc";

    public PlaceOrderTwoPCBffService(TwoPhaseCommitTransactionManager manager) {
        super(manager);
    }

    // Create Record
    public ResponseStatusDto placeOrder(OrderDto orderDto) throws CustomException {
        TwoPhaseCommitTransaction transaction = null;
        ProductDto productDto = ProductDto.builder()
                .id(orderDto.getProductId())
                .build();
        boolean isCommitted = false;

        try {
            transaction = manager.start();
            String transactionId = transaction.getId();
            log.info("Starting 2PC transaction: {}", transactionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ScalarDB-Transaction-ID", transactionId);


            // Get Inventory Info
            productDto = executeGet(urlInventory + "/" + productDto.getId(), headers, new ParameterizedTypeReference<ApiResponse<ProductDto>>() {});

            // Check Stock
            if(productDto.getStock() < orderDto.getOrderQty()){
                throw new RuntimeException("We are out of stock.");
            }
            // Set new stock value
            productDto.setStock(productDto.getStock() - orderDto.getOrderQty());

            executePut(urlInventory, productDto, headers);

            // Insert Order

            // Execute 2PC protocol
            executePost(urlOrder, orderDto, headers);

            executeTwoPcPhase(urlInventory + "/prepare", headers);
            executeTwoPcPhase(urlOrder + "/prepare", headers);

//            executeTwoPcPhase(urlInventory + "/validate", headers);
//            executeTwoPcPhase(urlOrder + "/validate", headers);

            executeTwoPcPhase(urlInventory + "/commit", headers);
            isCommitted = true;
            executeTwoPcPhase(urlOrder + "/commit", headers);

            log.info("2PC transaction committed: {}", transactionId);

            return ResponseStatusDto.builder().code(0).message("").build();
        } catch (Exception e) {
            log.error("2PC transaction failed: {}", e.getMessage(), e);
            if (transaction != null ) {
                if( !isCommitted ){
                    handleTransactionRollback(transaction, BASE_URL4INVENTORY + "/rollback", transaction.getId());
                    handleTransactionRollback(transaction, BASE_URL4ORDER + "/rollback", transaction.getId());
                }else{
                    log.info("Some of tables need Lazy Recovery: {}", transaction.getId());
                }
            }
            throw new CustomException(e, determineErrorCode(e));
        }
    }

}
