package com.example.demo_multiple_tm_order.repository;

import com.example.demo_multiple_tm_order.model.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.scalar.db.api.*;
import com.scalar.db.exception.transaction.*;
import com.scalar.db.io.Key;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private int scanLimit = 100; // Default scan limit
    
    public void setScanLimit(int scanLimit) {
        this.scanLimit = scanLimit;
    }
    
    public int getScanLimit() {
        return scanLimit;
    }

    // Get Record by Partition & Clustering Key
    public Order getOrder(DistributedTransaction transaction, Order order) throws CrudException {
        Key partitionKey = order.getPartitionKey();
        
        Get get = Get.newBuilder()
            .namespace(Order.NAMESPACE)
            .table(Order.TABLE)
            .partitionKey(partitionKey)
            
            .projections(Order.ID, Order.PRODUCT_ID, Order.ORDER_QTY, Order.ORDER_DATETIME)
            .build();
        Optional<Result> result = transaction.get(get);
        if (result.isEmpty()) {
            throw new RuntimeException("No record found in Order");
        }
        return buildOrder(result.get());
    }

    // Insert Record
    public Order insertOrder(DistributedTransaction transaction, Order order) throws CrudException {
        Key partitionKey = order.getPartitionKey();
        
        Insert insert = Insert.newBuilder()
            .namespace(Order.NAMESPACE)
            .table(Order.TABLE)
            .partitionKey(partitionKey)
            .intValue(Order.PRODUCT_ID, order.getProductId())
            .intValue(Order.ORDER_QTY, order.getOrderQty())
            .timestampValue(Order.ORDER_DATETIME, order.getOrderDatetime())
            .build();
        transaction.insert(insert);
        return order;
    }

    // Update Record
    public Order updateOrder(DistributedTransaction transaction, Order order) throws CrudException {
        Key partitionKey = order.getPartitionKey();
        
        MutationCondition condition = ConditionBuilder.updateIfExists();

        Update update = Update.newBuilder()
            .namespace(Order.NAMESPACE)
            .table(Order.TABLE)
            .partitionKey(partitionKey)
            .intValue(Order.PRODUCT_ID, order.getProductId())
            .intValue(Order.ORDER_QTY, order.getOrderQty())
            .timestampValue(Order.ORDER_DATETIME, order.getOrderDatetime())
            .condition(condition)
            .build();
        transaction.update(update);
        return order;
    }

    // Upsert Record
    public Order upsertOrder(DistributedTransaction transaction, Order order) throws CrudException {
        Key partitionKey = order.getPartitionKey();
        
        Upsert upsert = Upsert.newBuilder()
            .namespace(Order.NAMESPACE)
            .table(Order.TABLE)
            .partitionKey(partitionKey)
            .intValue(Order.PRODUCT_ID, order.getProductId())
            .intValue(Order.ORDER_QTY, order.getOrderQty())
            .timestampValue(Order.ORDER_DATETIME, order.getOrderDatetime())
            .build();
        transaction.upsert(upsert);
        return order;
    }

    // Delete Record
    public void deleteOrder(DistributedTransaction transaction, Order order) throws CrudException {
        Key partitionKey = order.getPartitionKey();
        
        MutationCondition condition = ConditionBuilder.deleteIfExists();
        Delete delete = Delete.newBuilder()
            .namespace(Order.NAMESPACE)
            .table(Order.TABLE)
            .partitionKey(partitionKey)
            
            .condition(condition)
            .build();
        transaction.delete(delete);
    }

    // Scan All Records
    public List<Order> getOrderListAll(DistributedTransaction transaction) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(Order.NAMESPACE)
            .table(Order.TABLE)
            .all()
            .projections(Order.ID, Order.PRODUCT_ID, Order.ORDER_QTY, Order.ORDER_DATETIME)
            .limit(scanLimit)
            .build();
        List<Result> results = transaction.scan(scan);
        List<Order> orderList = new ArrayList<>();
        for (Result result : results) {
            orderList.add(buildOrder(result));
        }
        return orderList;
    }

    // Scan Records by Partition Key
    public List<Order> getOrderListByPk(DistributedTransaction transaction, Key partitionKey) throws CrudException {
        Scan scan = Scan.newBuilder()
            .namespace(Order.NAMESPACE)
            .table(Order.TABLE)
            .partitionKey(partitionKey)
            .projections(Order.ID, Order.PRODUCT_ID, Order.ORDER_QTY, Order.ORDER_DATETIME)
            .limit(scanLimit)
            .build();
        List<Result> results = transaction.scan(scan);
        List<Order> orderList = new ArrayList<>();
        for (Result result : results) {
            orderList.add(buildOrder(result));
        }
        return orderList;
    }

    // Object Builder from ScalarDB Result
    private Order buildOrder(Result result) {
        return Order.builder()
            .id(result.getText(Order.ID))
            .productId(result.getInt(Order.PRODUCT_ID))
            .orderQty(result.getInt(Order.ORDER_QTY))
            .orderDatetime(result.getTimestamp(Order.ORDER_DATETIME))
            .build();
    }
}