package org.example.onlinemart.service;

import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.OrderItem;

import java.util.List;

public interface OrderService {
    Order createOrder(int userId, List<OrderItem> items);
    Order cancelOrder(int orderId);
    Order completeOrder(int orderId);
    Order findById(int orderId);
    List<Order> findAll();
    List<Order> findByUserId(int userId);
}