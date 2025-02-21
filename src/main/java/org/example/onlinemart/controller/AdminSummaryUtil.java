package org.example.onlinemart.controller;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.Order.OrderStatus;
import org.example.onlinemart.entity.OrderItem;
import org.example.onlinemart.service.OrderService;

import java.util.*;

public class AdminSummaryUtil {

    private AdminSummaryUtil() {}

    public static AdminController.ProductStats findMostProfitableProduct(OrderService orderService, OrderItemDAO orderItemDAO) {

        Map<Integer, AdminController.ProductStats> profitMap = new HashMap<>();

        List<Order> all = orderService.findAll();
        for (Order o : all) {
            if (o.getOrderStatus() != OrderStatus.Completed) {
                continue;
            }
            int orderId = o.getOrderId();
            List<OrderItem> items = orderItemDAO.findByOrderId(orderId);

            for (OrderItem oi : items) {
                int pId = oi.getProduct().getProductId();
                double profitPerItem = oi.getRetailPriceSnapshot() - oi.getWholesalePriceSnapshot();
                double total = profitPerItem * oi.getQuantity();

                AdminController.ProductStats stats = profitMap.getOrDefault(pId,
                        new AdminController.ProductStats(pId, oi.getProduct().getProductName(), 0.0, 0));
                stats.setTotalProfit(stats.getTotalProfit() + total);
                stats.setTotalSold(stats.getTotalSold() + oi.getQuantity());
                profitMap.put(pId, stats);
            }
        }

        AdminController.ProductStats max = null;
        for (AdminController.ProductStats ps : profitMap.values()) {
            if (max == null || ps.getTotalProfit() > max.getTotalProfit()) {
                max = ps;
            }
        }
        return (max != null) ? max : new AdminController.ProductStats();
    }

    public static List<AdminController.ProductStats> findTop3Popular(OrderService orderService, OrderItemDAO orderItemDAO) {
        Map<Integer, AdminController.ProductStats> soldMap = new HashMap<>();

        List<Order> all = orderService.findAll();
        for (Order o : all) {
            if (o.getOrderStatus() != OrderStatus.Completed) {
                continue;
            }
            List<OrderItem> items = orderItemDAO.findByOrderId(o.getOrderId());
            for (OrderItem oi : items) {
                int pId = oi.getProduct().getProductId();
                AdminController.ProductStats stats = soldMap.getOrDefault(
                        pId,
                        new AdminController.ProductStats(
                                pId,
                                oi.getProduct().getProductName(),
                                0.0,
                                0
                        ));
                stats.setTotalSold(stats.getTotalSold() + oi.getQuantity());
                soldMap.put(pId, stats);
            }
        }

        List<AdminController.ProductStats> list = new ArrayList<>(soldMap.values());
        list.sort((a, b) -> {
            int cmp = Integer.compare(b.getTotalSold(), a.getTotalSold());
            if (cmp == 0) {
                return Integer.compare(a.getProductId(), b.getProductId());
            }
            return cmp;
        });

        return (list.size() > 3) ? list.subList(0, 3) : list;
    }

    public static int countTotalSold(OrderService orderService, OrderItemDAO orderItemDAO) {
        int total = 0;
        List<Order> all = orderService.findAll();
        for (Order o : all) {
            if (o.getOrderStatus() != OrderStatus.Completed) {
                continue;
            }
            List<OrderItem> items = orderItemDAO.findByOrderId(o.getOrderId());
            for (OrderItem oi : items) {
                total += oi.getQuantity();
            }
        }
        return total;
    }
}