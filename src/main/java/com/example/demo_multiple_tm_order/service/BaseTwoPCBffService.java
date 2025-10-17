package com.example.demo_multiple_tm_order.service;

import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.scalar.db.api.TwoPhaseCommitTransaction;
import com.scalar.db.api.TwoPhaseCommitTransactionManager;
import com.scalar.db.exception.transaction.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for Two-Phase Commit BFF Services
 *
 * This abstract class provides common functionality for coordinating distributed transactions
 * across multiple 2PC microservices by:
 * - Managing ScalarDB 2PC transaction lifecycle
 * - Providing reusable REST API call methods
 * - Handling 2PC protocol phases (prepare, validate, commit, rollback)
 * - Centralizing error handling and response validation
 *
 * All TwoPC BFF service classes should extend this base class to inherit these capabilities.
 */
@Slf4j
public abstract class BaseTwoPCBffService {

    @Autowired
    protected RestTemplate restTemplate;

    protected TwoPhaseCommitTransactionManager manager;

    protected BaseTwoPCBffService(TwoPhaseCommitTransactionManager manager) {
        this.manager = manager;
    }

    /**
     * Execute HTTP POST operation for insert/upsert operations
     *
     * @param url The target URL
     * @param dto The DTO object to send in the request body
     * @param headers HTTP headers including transaction ID
     * @param <T> The type of the DTO
     * @throws CustomException if the operation fails
     */
    protected <T> void executePost(String url, T dto, HttpHeaders headers) throws CustomException {
        HttpEntity<T> request = new HttpEntity<>(dto, headers);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        ApiResponse<Void> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "POST operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("POST operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }
    }

    /**
     * Execute HTTP PUT operation for update operations
     *
     * @param url The target URL
     * @param dto The DTO object to send in the request body
     * @param headers HTTP headers including transaction ID
     * @param <T> The type of the DTO
     * @throws CustomException if the operation fails
     */
    protected <T> void executePut(String url, T dto, HttpHeaders headers) throws CustomException {
        HttpEntity<T> request = new HttpEntity<>(dto, headers);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
            url,
            HttpMethod.PUT,
            request,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        ApiResponse<Void> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "PUT operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("PUT operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }
    }

    /**
     * Execute HTTP DELETE operation
     *
     * @param url The target URL (should include path parameters)
     * @param headers HTTP headers including transaction ID
     * @throws CustomException if the operation fails
     */
    protected void executeDelete(String url, HttpHeaders headers) throws CustomException {
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            request,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        ApiResponse<Void> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "DELETE operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("DELETE operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }
    }

    /**
     * Execute HTTP GET operation that returns data
     *
     * @param url The target URL
     * @param headers HTTP headers including transaction ID
     * @param typeRef ParameterizedTypeReference for the response type
     * @param <T> The type of data returned
     * @return The data from the response
     * @throws CustomException if the operation fails
     */
    protected <T> T executeGet(String url, HttpHeaders headers, ParameterizedTypeReference<ApiResponse<T>> typeRef) throws CustomException {
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<T>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            request,
            typeRef
        );

        ApiResponse<T> body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null || !body.isSuccess()) {
            String errorMsg = body != null ? body.getMessage() : "GET operation failed";
            Integer errorCode = body != null ? body.getErrorCode() : 9100;
            throw new CustomException("GET operation failed: " + errorMsg, errorCode != null ? errorCode : 9100);
        }

        return body.getData();
    }

    /**
     * Execute a 2PC phase operation (prepare, validate, commit, or rollback)
     *
     * @param url The phase endpoint URL
     * @param headers HTTP headers including transaction ID
     * @throws CustomException if the phase operation fails
     */
    protected void executeTwoPcPhase(String url, HttpHeaders headers) throws CustomException {
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            request,
            new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful() ||
            response.getBody() == null ||
            !response.getBody().isSuccess()) {
            throw new CustomException("2PC phase operation failed for: " + url, 9100);
        }
    }

    /**
     * Handle transaction rollback when an error occurs
     *
     * @param transaction The transaction to rollback
     * @param rollbackUrl The rollback endpoint URL
     * @param transactionId The transaction ID for logging
     */
    protected void handleTransactionRollback(TwoPhaseCommitTransaction transaction, String rollbackUrl, String transactionId) {
        if (transaction != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("ScalarDB-Transaction-ID", transactionId);

                HttpEntity<Void> rollbackRequest = new HttpEntity<>(headers);
                restTemplate.exchange(
                    rollbackUrl,
                    HttpMethod.GET,
                    rollbackRequest,
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
                );

                log.info("Transaction rolled back: {}", transactionId);
            } catch (Exception ex) {
                log.error("Rollback failed: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * Determine error code based on exception type
     *
     * @param e The exception
     * @return The appropriate error code
     */
    protected int determineErrorCode(Exception e) {
        if (e instanceof UnsatisfiedConditionException) return 9100;
        if (e instanceof UnknownTransactionStatusException) return 9200;
        if (e instanceof TransactionException) return 9300;
        if (e instanceof RuntimeException) return 9400;
        return 9500;
    }
}
