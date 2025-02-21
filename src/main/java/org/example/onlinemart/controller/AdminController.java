package org.example.onlinemart.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;

    public AdminController(ProductService productService,
                           OrderService orderService) {
        this.productService = productService;
        this.orderService = orderService;
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

    // etc...
}