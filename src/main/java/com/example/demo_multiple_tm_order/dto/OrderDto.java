package com.example.demo_multiple_tm_order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class OrderDto {
    private String id;
    private Integer productId;
    private Integer orderQty;
    @Schema(type = "string", format = "date-time", example = "2025-09-15T14:30:00")
    private LocalDateTime orderDatetime;
}