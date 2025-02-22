package org.example.onlinemart.controller;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dao.WatchlistDAO;
import org.example.onlinemart.dto.UserDTO; // <-- make sure you have this DTO in your codebase
import org.example.onlinemart.entity.*;
import org.example.onlinemart.service.OrderService;
import org.example.onlinemart.service.ProductService;
import org.example.onlinemart.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final WatchlistDAO watchlistDAO;
    private final OrderItemDAO orderItemDAO;

    public UserController(UserService userService,
                          ProductService productService,
                          OrderService orderService,
                          WatchlistDAO watchlistDAO,
                          OrderItemDAO orderItemDAO) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.watchlistDAO = watchlistDAO;
        this.orderItemDAO = orderItemDAO;
    }

    @GetMapping("/products")
    public List<ProductDTO> viewProducts() {
        List<Product> products = productService.findAllInStock();
        return products.stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/products/{productId}")
    public ProductDTO getProductDetail(@PathVariable int productId) {
        Product p = productService.findById(productId);
        if (p == null || p.getStock() <= 0) {
            throw new RuntimeException("Product not available");
        }
        return ProductDTO.fromEntity(p);
    }

    @PostMapping("/orders")
    public OrderDTO placeOrder(@RequestParam int userId,
                               @RequestBody List<OrderItem> orderItems) {
        Order order = orderService.createOrder(userId, orderItems);
        return OrderDTO.fromEntity(order);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public OrderDTO cancelOrder(@RequestParam int userId,
                                @PathVariable int orderId) {
        Order order = orderService.findById(orderId);
        if (order == null || order.getUser().getUserId() != userId) {
            throw new RuntimeException("Cannot access this order or not found");
        }
        order = orderService.cancelOrder(orderId);
        return OrderDTO.fromEntity(order);
    }

    @GetMapping("/orders")
    public List<OrderDTO> getUserOrders(@RequestParam int userId) {
        List<Order> orders = orderService.findByUserId(userId);
        return orders.stream()
                .map(OrderDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/orders/{orderId}")
    public OrderDTO getSingleOrder(@RequestParam int userId,
                                   @PathVariable int orderId) {
        Order order = orderService.findById(orderId);
        if (order == null || order.getUser().getUserId() != userId) {
            throw new RuntimeException("Cannot access this order or not found");
        }
        return OrderDTO.fromEntity(order);
    }

    @PostMapping("/watchlist")
    public String addToWatchlist(@RequestParam int userId,
                                 @RequestParam int productId) {
        Watchlist existing = watchlistDAO.findByUserAndProduct(userId, productId);
        if (existing != null) {
            throw new RuntimeException("Product already on watchlist");
        }

        User user = userService.findById(userId);
        Product product = productService.findById(productId);
        if (user == null || product == null) {
            throw new RuntimeException("User or product not found");
        }

        Watchlist w = new Watchlist();
        w.setUser(user);
        w.setProduct(product);
        w.setCreatedAt(new Date());
        watchlistDAO.save(w);

        return "Product " + productId + " added to watchlist.";
    }

    @DeleteMapping("/watchlist/{productId}")
    public String removeFromWatchlist(@RequestParam int userId,
                                      @PathVariable int productId) {
        Watchlist existing = watchlistDAO.findByUserAndProduct(userId, productId);
        if (existing == null) {
            throw new RuntimeException("Product not found in watchlist");
        }
        watchlistDAO.delete(existing);
        return "Removed product " + productId + " from watchlist.";
    }

    @GetMapping("/watchlist")
    public List<ProductDTO> viewWatchlist(@RequestParam int userId) {
        List<Product> watchlistProducts = userService.getWatchlistProductsInStock(userId);
        return watchlistProducts.stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/summary/top3-frequent")
    public List<ProductDTO> top3Frequent(@RequestParam int userId) {
        List<Order> completedOrders = orderService.findByUserId(userId).stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.Completed)
                .collect(Collectors.toList());

        Map<Integer, Integer> productCountMap = new HashMap<>();
        for (Order order : completedOrders) {
            List<OrderItem> items = orderItemDAO.findByOrderId(order.getOrderId());
            for (OrderItem oi : items) {
                int pid = oi.getProduct().getProductId();
                productCountMap.put(pid,
                        productCountMap.getOrDefault(pid, 0) + oi.getQuantity());
            }
        }

        List<Map.Entry<Integer, Integer>> sorted = productCountMap.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = b.getValue().compareTo(a.getValue());
                    return (cmp == 0) ? Integer.compare(a.getKey(), b.getKey()) : cmp;
                })
                .collect(Collectors.toList());

        List<Integer> topIds = sorted.stream()
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return topIds.stream()
                .map(id -> productService.findById(id))
                .filter(Objects::nonNull)
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/summary/top3-recent")
    public List<ProductDTO> top3Recent(@RequestParam int userId) {
        List<Order> completedOrders = orderService.findByUserId(userId).stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.Completed)
                .sorted((o1, o2) -> o2.getOrderTime().compareTo(o1.getOrderTime()))
                .collect(Collectors.toList());

        List<OrderItem> allItems = new ArrayList<>();
        for (Order order : completedOrders) {
            allItems.addAll(orderItemDAO.findByOrderId(order.getOrderId()));
        }

        allItems.sort((i1, i2) -> {
            Date t1 = i1.getOrder().getOrderTime();
            Date t2 = i2.getOrder().getOrderTime();
            int cmp = t2.compareTo(t1);
            if (cmp == 0) {
                return Integer.compare(
                        i1.getProduct().getProductId(),
                        i2.getProduct().getProductId()
                );
            }
            return cmp;
        });

        List<ProductDTO> result = new ArrayList<>();
        Set<Integer> seenProductIds = new HashSet<>();
        for (OrderItem item : allItems) {
            int pid = item.getProduct().getProductId();
            if (!seenProductIds.contains(pid)) {
                result.add(ProductDTO.fromEntity(item.getProduct()));
                seenProductIds.add(pid);
            }
            if (result.size() == 3) {
                break;
            }
        }
        return result;
    }

    public static class ProductDTO {
        private int productId;
        private String productName;
        private String description;
        private double retailPrice;

        public ProductDTO() {
        }

        public static ProductDTO fromEntity(Product p) {
            if (p == null) return null;
            ProductDTO dto = new ProductDTO();
            dto.setProductId(p.getProductId());
            dto.setProductName(p.getProductName());
            dto.setDescription(p.getDescription());
            dto.setRetailPrice(p.getRetailPrice());
            return dto;
        }

        // Getters and setters
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
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public double getRetailPrice() {
            return retailPrice;
        }
        public void setRetailPrice(double retailPrice) {
            this.retailPrice = retailPrice;
        }
    }

    public static class OrderDTO {
        private int orderId;
        private int userId;
        private UserDTO user;
        private String orderStatus;
        private Date orderTime;
        private Date updatedAt;

        public OrderDTO() {
        }

        public static OrderDTO fromEntity(Order order) {
            if (order == null) return null;
            OrderDTO dto = new OrderDTO();
            dto.setOrderId(order.getOrderId());
            dto.setUserId(order.getUser().getUserId());
            dto.setUser(UserDTO.fromEntity(order.getUser()));
            dto.setOrderStatus(order.getOrderStatus().toString());
            dto.setOrderTime(order.getOrderTime());
            dto.setUpdatedAt(order.getUpdatedAt());
            return dto;
        }

        public int getOrderId() {
            return orderId;
        }
        public void setOrderId(int orderId) {
            this.orderId = orderId;
        }
        public int getUserId() {
            return userId;
        }
        public void setUserId(int userId) {
            this.userId = userId;
        }
        public UserDTO getUser() {
            return user;
        }
        public void setUser(UserDTO user) {
            this.user = user;
        }
        public String getOrderStatus() {
            return orderStatus;
        }
        public void setOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
        }
        public Date getOrderTime() {
            return orderTime;
        }
        public void setOrderTime(Date orderTime) {
            this.orderTime = orderTime;
        }
        public Date getUpdatedAt() {
            return updatedAt;
        }
        public void setUpdatedAt(Date updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}