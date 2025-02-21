package org.example.onlinemart.dao;

import org.example.onlinemart.entity.Order;

import java.util.List;

public interface OrderDAO {
    void save(Order order);
    void update(Order order);
    Order findById(int orderId);
    List<Order> findAll();
    List<Order> findByUserId(int userId);
    List<Order> findAllPaginated(int offset, int limit);
}
