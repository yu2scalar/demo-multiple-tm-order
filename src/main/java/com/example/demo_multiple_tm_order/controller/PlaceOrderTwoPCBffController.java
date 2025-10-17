package com.example.demo_multiple_tm_order.controller;

import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.example.demo_multiple_tm_order.service.PlaceOrderTwoPCBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Two-Phase Commit BFF Controller for Order
 *
 * This BFF controller coordinates distributed transactions across multiple 2PC microservices.
 * It starts a transaction, calls 2PC endpoints with transaction ID propagation, and orchestrates
 * the 2PC protocol (prepare, validate, commit/rollback).
 *
 * Key differences from standard controller:
 * - Orchestrates calls to multiple 2PC services
 * - Manages 2PC lifecycle (start, propagate, prepare, validate, commit/rollback)
 * - No SQL execution endpoints
 */
@RequestMapping(value = "/place-order-two-pc-bff")
@RestController
public class PlaceOrderTwoPCBffController {
    @Autowired
    private PlaceOrderTwoPCBffService placeOrderTwoPCBffService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> placeOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = placeOrderTwoPCBffService.placeOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }


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
