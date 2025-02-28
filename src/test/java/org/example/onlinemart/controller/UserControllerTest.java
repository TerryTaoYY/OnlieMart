package org.example.onlinemart.controller;

import lombok.*;
import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dao.WatchlistDAO;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.User;
import org.example.onlinemart.entity.Watchlist;
import org.example.onlinemart.service.OrderService;
import org.example.onlinemart.service.ProductService;
import org.example.onlinemart.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Mock
    private WatchlistDAO watchlistDAO;

    @Mock
    private OrderItemDAO orderItemDAO;

    @InjectMocks
    private UserController userController;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setProductId(100);
        sampleProduct.setProductName("Mocked Product");
        sampleProduct.setStock(5);
    }

    @Test
    void testViewProducts_Success() {
        when(productService.findAllInStock()).thenReturn(Collections.singletonList(sampleProduct));

        var products = userController.viewProducts();
        assertEquals(1, products.size());
        assertEquals("Mocked Product", products.get(0).getProductName());
        verify(productService, times(1)).findAllInStock();
    }

    @Test
    void testGetProductDetail_Success() {
        when(productService.findById(100)).thenReturn(sampleProduct);

        var dto = userController.getProductDetail(100);
        assertNotNull(dto);
        assertEquals("Mocked Product", dto.getProductName());
        verify(productService, times(1)).findById(100);
    }

    @Test
    void testGetProductDetail_NotAvailable() {
        Product outOfStockProduct = new Product();
        outOfStockProduct.setStock(0);
        when(productService.findById(100)).thenReturn(outOfStockProduct);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userController.getProductDetail(100));
        assertEquals("Product not available", ex.getMessage());
        verify(productService, times(1)).findById(100);
    }

    @Test
    void testGetProductDetail_NotFound() {
        when(productService.findById(999)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userController.getProductDetail(999));
        assertEquals("Product not available", ex.getMessage());
        verify(productService, times(1)).findById(999);
    }

    @Test
    void testGetUserOrders_Success() {
        Order order = new Order();
        order.setOrderId(1);
        order.setUser(new User());

        when(orderService.findByUserId(123)).thenReturn(Collections.singletonList(order));

        var orders = userController.getUserOrders(123);
        assertEquals(1, orders.size());
        assertEquals(1, orders.get(0).getOrderId());
        verify(orderService, times(1)).findByUserId(123);
    }

    @Test
    void testGetUserOrders_Empty() {
        when(orderService.findByUserId(123)).thenReturn(Collections.emptyList());

        var orders = userController.getUserOrders(123);
        assertTrue(orders.isEmpty());
        verify(orderService, times(1)).findByUserId(123);
    }

    @Test
    void testAddToWatchlist_Success() {
        User user = new User();
        user.setUserId(123);
        when(userService.findById(123)).thenReturn(user);
        when(productService.findById(100)).thenReturn(sampleProduct);
        when(watchlistDAO.findByUserAndProduct(123, 100)).thenReturn(null);

        String msg = userController.addToWatchlist(123, 100);
        assertEquals("Product 100 added to watchlist.", msg);
        verify(watchlistDAO, times(1)).save(any());
    }

    @Test
    void testAddToWatchlist_AlreadyExists() {
        Watchlist existingWatchlist = new Watchlist();
        existingWatchlist.setWatchlistId(1);
        User user = new User();
        user.setUserId(123);
        existingWatchlist.setUser(user);
        existingWatchlist.setProduct(sampleProduct);

        when(watchlistDAO.findByUserAndProduct(eq(123), eq(100))).thenReturn(existingWatchlist);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userController.addToWatchlist(123, 100));
        assertEquals("Product already on watchlist", ex.getMessage());
    }
}