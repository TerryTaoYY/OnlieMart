package org.example.onlinemart.controller;

import org.example.onlinemart.dao.WatchlistDAO;
import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.entity.*;
import org.example.onlinemart.service.ProductService;
import org.example.onlinemart.service.UserService;
import org.example.onlinemart.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Demonstrates user-related endpoints.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    // For watchlist logic (no dedicated service here)
    private final WatchlistDAO watchlistDAO;
    // For top-3 queries
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

    // ------------------------------------------------------------------
    // 1) View all in-stock products, but hide stock from the response
    // ------------------------------------------------------------------
    @GetMapping("/products")
    public List<ProductDTO> viewProducts() {
        List<Product> products = productService.findAllInStock();
        return products.stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Optionally, user can get single product detail (hide stock).
    @GetMapping("/products/{productId}")
    public ProductDTO getProductDetail(@PathVariable int productId) {
        Product p = productService.findById(productId);
        if (p == null || p.getStock() <= 0) {
            // product not found or out of stock for user
            throw new RuntimeException("Product not available");
        }
        return ProductDTO.fromEntity(p);
    }

    // ------------------------------------------------------------------
    // 2) Place a new order (already existing endpoint in your code)
    //    Using userId as param is risky, so we do a quick approach here.
    // ------------------------------------------------------------------
    @PostMapping("/orders")
    public Order placeOrder(@RequestParam int userId,
                            @RequestBody List<OrderItem> orderItems) {
        // In real code, you'd parse userId from JWT claims, not from param.
        return orderService.createOrder(userId, orderItems);
    }

    // ------------------------------------------------------------------
    // 3) Cancel an existing order
    // ------------------------------------------------------------------
    @PatchMapping("/orders/{orderId}/cancel")
    public Order cancelOrder(@RequestParam int userId,
                             @PathVariable int orderId) {
        // Confirm that this order belongs to the same user (or throw).
        Order order = orderService.findById(orderId);
        if (order == null || order.getUser().getUserId() != userId) {
            throw new RuntimeException("Cannot access this order or not found");
        }
        return orderService.cancelOrder(orderId);
    }

    // ------------------------------------------------------------------
    // 4) View all orders for current user
    // ------------------------------------------------------------------
    @GetMapping("/orders")
    public List<Order> getUserOrders(@RequestParam int userId) {
        // In real code, you'd get userId from JWT principal, not from param
        return orderService.findByUserId(userId);
    }

    // ------------------------------------------------------------------
    // 5) View single order detail (confirm user can access it)
    // ------------------------------------------------------------------
    @GetMapping("/orders/{orderId}")
    public Order getSingleOrder(@RequestParam int userId,
                                @PathVariable int orderId) {
        Order order = orderService.findById(orderId);
        if (order == null || order.getUser().getUserId() != userId) {
            throw new RuntimeException("Cannot access this order or not found");
        }
        return order;
    }

    // ------------------------------------------------------------------
    // Watchlist endpoints
    // ------------------------------------------------------------------

    // Add product to watchlist
    @PostMapping("/watchlist")
    public String addToWatchlist(@RequestParam int userId,
                                 @RequestParam int productId) {
        // Check if watchlist already has it
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

    // Remove product from watchlist
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

    // View all in-stock products in watchlist
    @GetMapping("/watchlist")
    public List<ProductDTO> viewWatchlist(@RequestParam int userId) {
        List<Watchlist> items = watchlistDAO.findByUserId(userId);
        // Filter out products that are out of stock
        return items.stream()
                .map(Watchlist::getProduct)
                .filter(p -> p.getStock() > 0)
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // 6) Top 3 most frequently purchased items (excluding canceled)
    // ------------------------------------------------------------------
    @GetMapping("/summary/top3-frequent")
    public List<ProductDTO> top3Frequent(@RequestParam int userId) {
        // Example logic:
        // 1) Get all "Completed" orders for the user
        // 2) Collect all order items
        // 3) Count total purchased quantity per product
        // 4) Sort descending, pick top 3
        List<Order> completedOrders = orderService.findByUserId(userId).stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.Completed)
                .collect(Collectors.toList());

        // For each completed order, fetch its items
        Map<Integer, Integer> productCountMap = new HashMap<>();
        for (Order order : completedOrders) {
            List<OrderItem> items = orderItemDAO.findByOrderId(order.getOrderId());
            for (OrderItem oi : items) {
                int productId = oi.getProduct().getProductId();
                productCountMap.put(productId,
                        productCountMap.getOrDefault(productId, 0) + oi.getQuantity());
            }
        }

        // Sort by quantity desc, then productId asc as tie-breaker
        List<Map.Entry<Integer, Integer>> sorted = productCountMap.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = b.getValue().compareTo(a.getValue());
                    if (cmp == 0) {
                        // tie break on productId ascending
                        return Integer.compare(a.getKey(), b.getKey());
                    }
                    return cmp;
                })
                .collect(Collectors.toList());

        // take top 3
        List<Integer> topIds = sorted.stream()
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // convert to ProductDTO
        return topIds.stream()
                .map(id -> productService.findById(id))
                .filter(Objects::nonNull)
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // 7) Top 3 most recently purchased items (excluding canceled)
    //    We'll rely on "order_time" descending.
    // ------------------------------------------------------------------
    @GetMapping("/summary/top3-recent")
    public List<ProductDTO> top3Recent(@RequestParam int userId) {
        // A naive approach:
        // 1) Gather all completed orders, sorted by orderTime desc
        // 2) For each order, gather items (also consider item ID as a tiebreak)
        // 3) Flatten into chronological list, pick top 3 distinct items
        // (Implementation can vary. We'll do something simplistic.)

        List<Order> completedOrders = orderService.findByUserId(userId).stream()
                .filter(o -> o.getOrderStatus() == Order.OrderStatus.Completed)
                .sorted((o1, o2) -> o2.getOrderTime().compareTo(o1.getOrderTime())) // newest first
                .collect(Collectors.toList());

        // Flatten
        List<OrderItem> allItems = new ArrayList<>();
        for (Order order : completedOrders) {
            allItems.addAll(orderItemDAO.findByOrderId(order.getOrderId()));
        }

        // Sort items by their order's orderTime desc, then by productId asc
        allItems.sort((i1, i2) -> {
            Date t1 = i1.getOrder().getOrderTime();
            Date t2 = i2.getOrder().getOrderTime();
            int cmp = t2.compareTo(t1); // descending
            if (cmp == 0) {
                // tie break on product ID ascending
                return Integer.compare(i1.getProduct().getProductId(),
                        i2.getProduct().getProductId());
            }
            return cmp;
        });

        // Now pick the first 3 distinct product IDs
        List<ProductDTO> result = new ArrayList<>();
        Set<Integer> seenProductIds = new HashSet<>();
        for (OrderItem item : allItems) {
            int pId = item.getProduct().getProductId();
            if (!seenProductIds.contains(pId)) {
                result.add(ProductDTO.fromEntity(item.getProduct()));
                seenProductIds.add(pId);
            }
            if (result.size() == 3) {
                break;
            }
        }

        return result;
    }

    // ============== DTO to hide product stock from buyer ==============
    public static class ProductDTO {
        private int productId;
        private String productName;
        private String description;
        private double retailPrice;
        // We do NOT expose stock, wholesalePrice, etc.

        // constructor(s), getters, setters omitted for brevity
        public static ProductDTO fromEntity(Product p) {
            ProductDTO dto = new ProductDTO();
            dto.setProductId(p.getProductId());
            dto.setProductName(p.getProductName());
            dto.setDescription(p.getDescription());
            dto.setRetailPrice(p.getRetailPrice());
            return dto;
        }

        public ProductDTO() {
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
}