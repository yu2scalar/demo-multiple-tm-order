package com.example.demo_multiple_tm_order.controller;

import com.example.demo_multiple_tm_order.service.OrderTwoPCService;
import com.example.demo_multiple_tm_order.service.BaseTwoPCService;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Two-Phase Commit (2PC) Controller for Order
 *
 * This controller extends BaseTwoPCController and provides CRUD endpoints.
 * It receives a transaction ID via the ScalarDB-Transaction-ID header.
 *
 * Key characteristics:
 * - All CRUD methods accept ScalarDB-Transaction-ID header
 * - Transaction is joined in the service layer using manager.join()
 * - Transaction commit is handled by BFF via /commit endpoint
 * - Inherits lifecycle endpoints (prepare, validate, commit, rollback) from BaseTwoPCController
 */
@RequestMapping(value = "/order-two-pc")
@RestController
public class OrderTwoPCController extends BaseTwoPCController {
    @Autowired
    private OrderTwoPCService orderService;

    @Override
    protected BaseTwoPCService getService() {
        return orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertOrder(
            @RequestBody OrderDto orderDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = orderService.insertOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertOrder(
            @RequestBody OrderDto orderDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = orderService.upsertOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(
            @PathVariable("id") String id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        OrderDto result = orderService.getOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateOrder(
            @RequestBody OrderDto orderDto,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        ResponseStatusDto status = orderService.updateOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable("id") String id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = orderService.deleteOrder(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderByPk(
            @PathVariable("id") String id,
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        List<OrderDto> result = orderService.getOrderListByPk(orderDto, transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderListAll(
            @RequestHeader("ScalarDB-Transaction-ID") String transactionId) {
        List<OrderDto> result = orderService.getOrderListAll(transactionId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
