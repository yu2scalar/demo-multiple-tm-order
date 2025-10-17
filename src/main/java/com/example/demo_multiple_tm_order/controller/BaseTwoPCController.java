package com.example.demo_multiple_tm_order.controller;

import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.example.demo_multiple_tm_order.service.BaseTwoPCService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Base controller for Two-Phase Commit (2PC) REST endpoints
 *
 * Provides common lifecycle endpoints for 2PC protocol:
 * - GET /prepare - Prepares the transaction
 * - GET /validate - Validates the transaction
 * - GET /commit - Commits the transaction
 * - GET /rollback - Rolls back the transaction
 *
 * All endpoints receive transaction ID via ScalarDB-Transaction-ID header.
 * Concrete controllers extend this and implement getService().
 */
public abstract class BaseTwoPCController {

    /**
     * Get the service instance for this controller
     * Must be implemented by concrete controllers
     */
    protected abstract BaseTwoPCService getService();

    /**
     * Prepare endpoint - prepares the transaction for commit
     */
    @GetMapping("/prepare")
    public ResponseEntity<ApiResponse<Void>> prepare(
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) throws CustomException {
        ResponseStatusDto status = getService().prepare(transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    /**
     * Validate endpoint - validates the transaction state
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Void>> validate(
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) throws CustomException {
        ResponseStatusDto status = getService().validate(transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    /**
     * Commit endpoint - commits the transaction
     */
    @GetMapping("/commit")
    public ResponseEntity<ApiResponse<Void>> commit(
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) throws CustomException {
        ResponseStatusDto status = getService().commit(transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    /**
     * Rollback endpoint - rolls back the transaction
     */
    @GetMapping("/rollback")
    public ResponseEntity<ApiResponse<Void>> rollback(
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) throws CustomException {
        ResponseStatusDto status = getService().rollback(transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    /**
     * Exception handler for CustomException
     */
    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleScalarDbException(CustomException ex) {
        ApiResponse<Void> errorResponse = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        return switch (ex.getErrorCode()) {
            case 9100, 9400 -> new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            case 9200, 9300 -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            default -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }
}
