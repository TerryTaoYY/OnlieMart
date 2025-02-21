package org.example.onlinemart.controller;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.OrderService;
import org.example.onlinemart.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    // NEW: orderItemDAO for summary queries
    private final OrderItemDAO orderItemDAO;

    public AdminController(ProductService productService,
                           OrderService orderService,
                           OrderItemDAO orderItemDAO) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderItemDAO = orderItemDAO;
    }

    // For example, add a new product
    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        productService.save(product);
        return product;
    }

    // Update an existing product
    @PatchMapping("/products/{productId}")
    public Product updateProduct(@PathVariable int productId,
                                 @RequestBody Product updated) {
        return productService.updateProductFields(productId, updated);
    }

    // Mark order as Completed
    @PatchMapping("/orders/{orderId}/complete")
    public Order completeOrder(@PathVariable int orderId) {
        return orderService.completeOrder(orderId);
    }

    // Cancel order
    @PatchMapping("/orders/{orderId}/cancel")
    public Order cancelOrder(@PathVariable int orderId) {
        return orderService.cancelOrder(orderId);
    }

    // View orders, possibly with pagination
    @GetMapping("/orders")
    public List<Order> listOrders() {
        return orderService.findAll();
    }

    // ===================== Admin Summaries =====================

    @GetMapping("/summary/most-profit")
    public ProductStats mostProfitableProduct() {
        // Now we pass orderItemDAO so items can be fetched from DB
        return AdminSummaryUtil.findMostProfitableProduct(orderService, orderItemDAO);
    }

    @GetMapping("/summary/top3-popular")
    public List<ProductStats> top3PopularProducts() {
        return AdminSummaryUtil.findTop3Popular(orderService, orderItemDAO);
    }

    @GetMapping("/summary/total-sold")
    public int totalItemsSold() {
        return AdminSummaryUtil.countTotalSold(orderService, orderItemDAO);
    }

    // Just a tiny data model to hold product ID and aggregated stats.
    public static class ProductStats {
        private int productId;
        private String productName;
        private double totalProfit;
        private int totalSold;

        public ProductStats() {}

        public ProductStats(int productId, String productName, double totalProfit, int totalSold) {
            this.productId = productId;
            this.productName = productName;
            this.totalProfit = totalProfit;
            this.totalSold = totalSold;
        }

        // getters, setters
        public int getProductId() {
            return productId;
        }
        public void setProductId(int productId) {
            this.productId = productId;
        }
        public String getProductName() {
            return productName;
        }
        public void setProductName(String productName) {
            this.productName = productName;
        }
        public double getTotalProfit() {
            return totalProfit;
        }
        public void setTotalProfit(double totalProfit) {
            this.totalProfit = totalProfit;
        }
        public int getTotalSold() {
            return totalSold;
        }
        public void setTotalSold(int totalSold) {
            this.totalSold = totalSold;
        }
    }
}