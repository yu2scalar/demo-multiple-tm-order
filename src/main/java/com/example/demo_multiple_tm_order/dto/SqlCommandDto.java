package com.example.demo_multiple_tm_order.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SqlCommandDto {
    private String sqlCommand;
}
