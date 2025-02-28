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
import org.example.onlinemart.exception.NotEnoughInventoryException;
import org.example.onlinemart.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.Hibernate;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;
    private final ProductDAO productDAO;
    private final UserDAO userDAO;

    public OrderServiceImpl(OrderDAO orderDAO, OrderItemDAO orderItemDAO,
                            ProductDAO productDAO, UserDAO userDAO) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
        this.productDAO = productDAO;
        this.userDAO = userDAO;
    }

    @Override
    public Order createOrder(int userId, List<OrderItem> items) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.Processing);
        order.setOrderTime(new Date());
        orderDAO.save(order);

        for (OrderItem oi : items) {
            Product product = productDAO.findById(oi.getProduct().getProductId());
            if (product == null) {
                throw new RuntimeException("Product not found");
            }
            int requestedQty = oi.getQuantity();
            if (requestedQty > product.getStock()) {
                throw new NotEnoughInventoryException("Not enough inventory for product ID: "
                        + product.getProductId());
            }

            product.setStock(product.getStock() - requestedQty);
            productDAO.update(product);

            oi.setOrder(order);
            oi.setProduct(product);
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

            if (order.getUser() != null) {
                Hibernate.initialize(order.getUser());
            }
            return order;
        }

        List<OrderItem> items = orderItemDAO.findByOrderId(orderId);
        for (OrderItem oi : items) {
            Product product = oi.getProduct();
            product.setStock(product.getStock() + oi.getQuantity());
            productDAO.update(product);
        }

        order.setOrderStatus(OrderStatus.Canceled);
        orderDAO.update(order);

        if (order.getUser() != null) {
            Hibernate.initialize(order.getUser());
        }

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
            if (order.getUser() != null) {
                Hibernate.initialize(order.getUser());
            }
            return order;
        }

        order.setOrderStatus(OrderStatus.Completed);
        orderDAO.update(order);

        if (order.getUser() != null) {
            Hibernate.initialize(order.getUser());
        }

        return order;
    }

    @Override
    public Order findById(int orderId) {
        Order order = orderDAO.findById(orderId);
        if (order != null && order.getUser() != null) {
            Hibernate.initialize(order.getUser());
        }
        return order;
    }


    @Override
    public List<Order> findAll() {
        return orderDAO.findAll();
    }

    @Override
    public List<Order> findByUserId(int userId) {
        List<Order> orders = orderDAO.findByUserId(userId);
        for (Order o : orders) {
            if (o.getUser() != null) {
                Hibernate.initialize(o.getUser());
            }
        }
        return orders;
    }

    @Override
    public List<Order> findAllPaginated(int offset, int limit) {
        return orderDAO.findAllPaginated(offset, limit);
    }
}