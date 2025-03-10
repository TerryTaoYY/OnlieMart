package org.example.onlinemart.controller;

import org.example.onlinemart.controller.AdminController.ProductStats;
import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dto.OrderDTO;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.OrderService;
import org.example.onlinemart.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderItemDAO orderItemDAO;

    @InjectMocks
    private AdminController adminController;

    private Product sampleProduct;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setProductId(1);
        sampleProduct.setProductName("Test Product");
        sampleProduct.setStock(10);

        sampleOrder = new Order();
        sampleOrder.setOrderId(1);
    }

    @Test
    void testAddProduct_Success() {
        when(productService.save(any(Product.class))).thenReturn(sampleProduct);

        Product result = adminController.addProduct(sampleProduct);
        assertNotNull(result);
        assertEquals("Test Product", result.getProductName());
        verify(productService, times(1)).save(any(Product.class));
    }

    @Test
    void testAddProduct_Failure() {
        when(productService.save(any(Product.class))).thenThrow(new RuntimeException("Save failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminController.addProduct(sampleProduct));
        assertEquals("Save failed", ex.getMessage());
        verify(productService, times(1)).save(sampleProduct);
    }

    @Test
    void testCompleteOrder_Success() {
        when(orderService.completeOrder(1)).thenReturn(sampleOrder);

        OrderDTO dto = adminController.completeOrder(1);
        assertNotNull(dto);
        assertEquals(1, dto.getOrderId());
        verify(orderService, times(1)).completeOrder(1);
    }

    @Test
    void testCompleteOrder_OrderNotFound() {
        when(orderService.completeOrder(anyInt()))
                .thenThrow(new RuntimeException("Order not found"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminController.completeOrder(999));
        assertEquals("Order not found", ex.getMessage());
        verify(orderService, times(1)).completeOrder(999);
    }

    @Test
    void testListAllProducts_Success() {
        when(productService.findAll()).thenReturn(Collections.singletonList(sampleProduct));

        List<Product> products = adminController.listAllProducts();
        assertEquals(1, products.size());
        verify(productService, times(1)).findAll();
    }

    @Test
    void testListAllProducts_Empty() {
        when(productService.findAll()).thenReturn(Collections.emptyList());

        List<Product> products = adminController.listAllProducts();
        assertTrue(products.isEmpty());
        verify(productService, times(1)).findAll();
    }

    @Test
    void testMostProfitableProduct_NoOrders() {
        when(orderService.findAll()).thenReturn(Collections.emptyList());
        ProductStats stats = adminController.mostProfitableProduct();
        assertNotNull(stats);
        assertEquals(0, stats.getProductId());
    }
}