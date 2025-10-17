package com.example.demo_multiple_tm_order.mapper;

import com.example.demo_multiple_tm_order.model.Order;
import com.example.demo_multiple_tm_order.dto.OrderDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class OrderMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static OrderDto mapToOrderDto(Order order) {
        return modelMapper.map(order, OrderDto.class);
    }

    // Convert DTO to Model
    public static Order mapToOrder(OrderDto orderDto) {
        return modelMapper.map(orderDto, Order.class);
    }

    // Convert Model List to DTO List
    public static List<OrderDto> mapToOrderDtoList(List<Order> orderList) {
        List<OrderDto> orderDtoList = new ArrayList<>();
        for (Order order : orderList) {
            orderDtoList.add(mapToOrderDto(order));
        }
        return orderDtoList;
    }
}