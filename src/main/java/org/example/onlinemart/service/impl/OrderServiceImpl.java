package org.example.onlinemart.service.impl;

import org.example.onlinemart.dao.OrderDAO;
import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dao.ProductDAO;
import org.example.onlinemart.dao.UserDAO;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.OrderItem;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.User;
import org.example.onlinemart.entity.Order.OrderStatus;
import org.example.onlinemart.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;
    private final ProductDAO productDAO;
    private final UserDAO userDAO;

    public OrderServiceImpl(OrderDAO orderDAO,
                            OrderItemDAO orderItemDAO,
                            ProductDAO productDAO,
                            UserDAO userDAO) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
        this.productDAO = productDAO;
        this.userDAO = userDAO;
    }

    @Override
    public Order createOrder(int userId, List<OrderItem> items) {
        // fetch user
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // create order
        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(Order.OrderStatus.Processing);
        order.setOrderTime(new Date());
        orderDAO.save(order);

        // for each item, deduct stock, save snapshots
        for (OrderItem oi : items) {
            Product product = productDAO.findById(oi.getProduct().getProductId());
            if (product == null) {
                throw new RuntimeException("Product not found");
            }
            int requestedQty = oi.getQuantity();
            if (requestedQty > product.getStock()) {
                // revert everything by throwing an exception
                throw new RuntimeException("NotEnoughInventoryException");
            }
            // deduct stock
            product.setStock(product.getStock() - requestedQty);
            productDAO.update(product);

            // set order ref
            oi.setOrder(order);
            oi.setProduct(product);
            // snapshot
            oi.setWholesalePriceSnapshot(product.getWholesalePrice());
            oi.setRetailPriceSnapshot(product.getRetailPrice());
            orderItemDAO.save(oi);
        }
        return order;
    }

    @Override
    public Order cancelOrder(int orderId) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        if (order.getOrderStatus() == OrderStatus.Completed) {
            throw new RuntimeException("Cannot cancel a completed order");
        }
        if (order.getOrderStatus() == OrderStatus.Canceled) {
            // already canceled
            return order;
        }
        // revert stock
        List<OrderItem> items = orderItemDAO.findByOrderId(orderId);
        for (OrderItem oi : items) {
            Product product = oi.getProduct();
            product.setStock(product.getStock() + oi.getQuantity());
            productDAO.update(product);
        }
        order.setOrderStatus(OrderStatus.Canceled);
        orderDAO.update(order);
        return order;
    }

    @Override
    public Order completeOrder(int orderId) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        if (order.getOrderStatus() == OrderStatus.Canceled) {
            throw new RuntimeException("Cannot complete a canceled order");
        }
        if (order.getOrderStatus() == OrderStatus.Completed) {
            // already completed
            return order;
        }
        order.setOrderStatus(OrderStatus.Completed);
        orderDAO.update(order);
        return order;
    }

    @Override
    public Order findById(int orderId) {
        return orderDAO.findById(orderId);
    }

    @Override
    public List<Order> findAll() {
        return orderDAO.findAll();
    }

    @Override
    public List<Order> findByUserId(int userId) {
        return orderDAO.findByUserId(userId);
    }
}