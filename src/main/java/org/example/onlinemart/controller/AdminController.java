package org.example.onlinemart.controller;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dto.OrderDTO;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.OrderService;
import org.example.onlinemart.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final OrderItemDAO orderItemDAO;

    public AdminController(ProductService productService,
                           OrderService orderService,
                           OrderItemDAO orderItemDAO) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderItemDAO = orderItemDAO;
    }

    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        productService.save(product);
        return product;
    }

    @GetMapping("/products")
    public List<Product> listAllProducts() {
        return productService.findAll();
    }

    @PatchMapping("/products/{productId}")
    public Product updateProduct(@PathVariable int productId, @RequestBody Product updated) {
        return productService.updateProductFields(productId, updated);
    }

    @PatchMapping("/orders/{orderId}/complete")
    public OrderDTO completeOrder(@PathVariable int orderId) {
        Order completed = orderService.completeOrder(orderId);
        return OrderDTO.fromEntity(completed);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public OrderDTO cancelOrder(@PathVariable int orderId) {
        Order canceled = orderService.cancelOrder(orderId);
        return OrderDTO.fromEntity(canceled);
    }

    @GetMapping("/orders")
    public List<OrderDTO> listOrders(@RequestParam(required = false) Integer page) {
        if (page == null) {
            // Return all orders
            return orderService.findAll().stream()
                    .map(OrderDTO::fromEntity)
                    .collect(Collectors.toList());
        }
        int pageSize = 5;
        int currentPage = (page < 1) ? 1 : page;
        int offset = (currentPage - 1) * pageSize;

        return orderService.findAllPaginated(offset, pageSize).stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/orders/{orderId}")
    public OrderDTO viewSingleOrder(@PathVariable int orderId) {
        Order order = orderService.findById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found with ID " + orderId);
        }
        return OrderDTO.fromEntity(order);
    }

    @GetMapping("/summary/most-profit")
    public ProductStats mostProfitableProduct() {
        return AdminSummaryUtil.findMostProfitableProduct(orderService, orderItemDAO);
    }

    @GetMapping("/summary/top3-popular-users")
    public List<ProductStats> top3PopularProducts() {
        return AdminSummaryUtil.findTop3Popular(orderService, orderItemDAO);
    }

    @GetMapping("/summary/total-sold")
    public int totalItemsSold() {
        return AdminSummaryUtil.countTotalSold(orderService, orderItemDAO);
    }

    public static class ProductStats {
        private int productId;
        private String productName;
        private double totalProfit;
        private int totalSold;

        public ProductStats() {
        }

        public ProductStats(int productId, String productName,
                            double totalProfit, int totalSold) {
            this.productId = productId;
            this.productName = productName;
            this.totalProfit = totalProfit;
            this.totalSold = totalSold;
        }

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