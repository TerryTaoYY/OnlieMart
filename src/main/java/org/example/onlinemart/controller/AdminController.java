package org.example.onlinemart.controller;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dto.OrderDTO;
import org.example.onlinemart.dto.PopularProductResult;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.AdminSummaryService;
import org.example.onlinemart.service.OrderService;
import org.example.onlinemart.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final OrderItemDAO orderItemDAO;
    private final AdminSummaryService adminSummaryService;

    public AdminController(ProductService productService,
                           OrderService orderService,
                           OrderItemDAO orderItemDAO,
                           AdminSummaryService adminSummaryService) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderItemDAO = orderItemDAO;
        this.adminSummaryService = adminSummaryService;
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
            List<Order> orders = orderService.findAllCached();
            return orders.stream()
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
        return adminSummaryService.findMostProfitableProduct();
    }

    @GetMapping("/summary/admin-top3-popular")
    public List<PopularProductResult> getTop3PopularProducts() {
        List<Object[]> rows = orderItemDAO.findTop3Popular();
        List<PopularProductResult> results = new ArrayList<>();

        for (Object[] row : rows) {
            Product product = (Product) row[0];
            Long totalQty = (Long) row[1];
            results.add(new PopularProductResult(
                    (long) product.getProductId(),
                    product.getProductName(),
                    totalQty
            ));
        }
        return results;
    }

    @GetMapping("/summary/total-sold")
    public int totalItemsSold() {
        return adminSummaryService.countTotalSold();
    }

    public static class ProductStats {
        private int productId;
        private String productName;
        private double totalProfit;
        private int totalSold;

        public ProductStats() {
        }

        public ProductStats(int productId, String productName, double totalProfit, int totalSold) {
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