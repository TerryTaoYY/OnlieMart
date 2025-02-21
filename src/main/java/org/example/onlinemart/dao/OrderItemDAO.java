package org.example.onlinemart.dao;

import org.example.onlinemart.entity.OrderItem;

import java.util.List;

public interface OrderItemDAO {
    void save(OrderItem orderItem);
    void update(OrderItem orderItem);
    List<OrderItem> findByOrderId(int orderId);
    // Possibly more queries
}
