package org.example.onlinemart.controller;

import org.example.onlinemart.dao.UserService;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.OrderItem;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    public UserController(UserService userService,
                          ProductService productService,
                          OrderService orderService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
    }

    // View all in-stock products
    @GetMapping("/products")
    public List<Product> viewProducts() {
        return productService.findAllInStock();
    }

    // Example: place a new order
    @PostMapping("/orders")
    public Order placeOrder(@RequestParam int userId,
                            @RequestBody List<OrderItem> orderItems) {
        // In practice, you'd define a specialized request DTO
        // containing the productId and quantity to buy, etc.
        return orderService.createOrder(userId, orderItems);
    }

    // Cancel an existing order
    @PatchMapping("/orders/{orderId}/cancel")
    public Order cancelOrder(@PathVariable int orderId) {
        return orderService.cancelOrder(orderId);
    }

    // View all orders for current user
    @GetMapping("/orders")
    public List<Order> getUserOrders(@RequestParam int userId) {
        // In a real scenario, you'd get userId from the JWT token principal
        return orderService.findByUserId(userId);
    }

    // etc. For watchlist, you'd have endpoints like:
    // @PostMapping("/watchlist"), @DeleteMapping("/watchlist/{productId}"), etc.
}