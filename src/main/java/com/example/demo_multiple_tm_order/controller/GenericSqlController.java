package com.example.demo_multiple_tm_order.controller;

import com.example.demo_multiple_tm_order.service.GenericSqlService;
import com.example.demo_multiple_tm_order.dto.ApiResponse;
import com.example.demo_multiple_tm_order.dto.ResponseStatusDto;
import com.example.demo_multiple_tm_order.dto.SqlCommandDto;
import com.example.demo_multiple_tm_order.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@RequestMapping(value = "/generic-sql")
@RestController
public class GenericSqlController {
    @Autowired
    private GenericSqlService genericSqlService;

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> executeSQLGeneric(@RequestBody SqlCommandDto sqlCommandDto) {
        List<Map<String, Object>> result = genericSqlService.executeSQLGeneric(sqlCommandDto);
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
