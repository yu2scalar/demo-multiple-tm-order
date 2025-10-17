package com.example.demo_multiple_tm_order.controller;

import com.example.demo_multiple_tm_order.service.OrderTwoPCBffService;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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
@RequestMapping(value = "/order-two-pc-bff")
@RestController
public class OrderTwoPCBffController {
    @Autowired
    private OrderTwoPCBffService orderTwoPCBffService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderTwoPCBffService.insertOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderTwoPCBffService.upsertOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        OrderDto result = orderTwoPCBffService.getOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderTwoPCBffService.updateOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = orderTwoPCBffService.deleteOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderByPk(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        List<OrderDto> result = orderTwoPCBffService.getOrderListByPk(orderDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderListAll() {
        List<OrderDto> result = orderTwoPCBffService.getOrderListAll();
        return ResponseEntity.ok(ApiResponse.success(result));
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
