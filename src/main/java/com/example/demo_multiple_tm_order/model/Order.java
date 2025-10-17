package com.example.demo_multiple_tm_order.model;

import lombok.*;
import com.scalar.db.io.Key;
import java.time.*;
import java.nio.ByteBuffer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    public static final String NAMESPACE = "shopping";
    public static final String TABLE = "order";
    public static final String ID = "id";
    public static final String PRODUCT_ID = "product_id";
    public static final String ORDER_QTY = "order_qty";
    public static final String ORDER_DATETIME = "order_datetime";

    private String id;
    private Integer productId;
    private Integer orderQty;
    private LocalDateTime orderDatetime;

    public Key getPartitionKey() {
        return Key.newBuilder().addText(ID, getId()).build();
    }

}
