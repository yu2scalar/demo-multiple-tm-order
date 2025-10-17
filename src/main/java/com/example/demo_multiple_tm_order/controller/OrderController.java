package com.example.demo_multiple_tm_order.controller;

import com.example.demo_multiple_tm_order.service.OrderService;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.dto.SqlCommandDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import com.scalar.db.exception.transaction.CrudException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RequestMapping(value = "/order")
@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderService.insertOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderService.upsertOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrder(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        OrderDto result = orderService.getOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateOrder(@RequestBody OrderDto orderDto) {
        ResponseStatusDto status = orderService.updateOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        ResponseStatusDto status = orderService.deleteOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{id}")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderByPk(@PathVariable("id") String id) {
        OrderDto orderDto = OrderDto.builder()
            .id(id)
            .build();
        List<OrderDto> result = orderService.getOrderListByPk(orderDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<OrderDto>>> getOrderListAll() {
        List<OrderDto> result = orderService.getOrderListAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/execute-sql")
    public ResponseEntity<ApiResponse<List<OrderDto>>> executeSQL(@RequestBody SqlCommandDto sqlCommandDto) {
        List<OrderDto> result = orderService.executeSQL(sqlCommandDto);
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