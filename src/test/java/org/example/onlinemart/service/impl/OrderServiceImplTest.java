package org.example.onlinemart.service.impl;

import org.example.onlinemart.dao.*;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.OrderItem;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.User;
import org.example.onlinemart.entity.Order.OrderStatus;
import org.example.onlinemart.exception.NotEnoughInventoryException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderDAO orderDAO;
    @Mock private OrderItemDAO orderItemDAO;
    @Mock private ProductDAO productDAO;
    @Mock private UserDAO userDAO;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Product sampleProduct;
    private OrderItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1);

        sampleProduct = new Product();
        sampleProduct.setProductId(100);
        sampleProduct.setStock(10);

        sampleItem = new OrderItem();
        sampleItem.setProduct(sampleProduct);
        sampleItem.setQuantity(2);
    }

    @Test
    void testCreateOrder_Success() {
        when(userDAO.findById(1)).thenReturn(sampleUser);
        when(productDAO.findById(100)).thenReturn(sampleProduct);

        Order result = orderService.createOrder(1, Collections.singletonList(sampleItem));
        assertNotNull(result);
        verify(orderDAO, times(1)).save(any(Order.class));
        verify(orderItemDAO, times(1)).save(any(OrderItem.class));
        verify(productDAO, times(1)).update(sampleProduct);
        assertEquals(8, sampleProduct.getStock()); // 10 - 2
    }

    @Test
    void testCreateOrder_UserNotFound() {
        when(userDAO.findById(1)).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> orderService.createOrder(1, Collections.singletonList(sampleItem)));
    }

    @Test
    void testCreateOrder_NotEnoughInventory() {
        sampleItem.setQuantity(50);
        when(userDAO.findById(1)).thenReturn(sampleUser);
        when(productDAO.findById(100)).thenReturn(sampleProduct);

        assertThrows(NotEnoughInventoryException.class,
                () -> orderService.createOrder(1, Collections.singletonList(sampleItem)));
    }

    @Test
    void testCancelOrder_Success() {
        Order existingOrder = new Order();
        existingOrder.setOrderId(1000);
        existingOrder.setOrderStatus(OrderStatus.Processing);

        when(orderDAO.findById(1000)).thenReturn(existingOrder);
        when(orderItemDAO.findByOrderId(1000)).thenReturn(Collections.singletonList(sampleItem));

        Order canceled = orderService.cancelOrder(1000);
        assertEquals(OrderStatus.Canceled, canceled.getOrderStatus());
        assertEquals(12, sampleProduct.getStock());
        verify(productDAO).update(sampleProduct);
        verify(orderDAO).update(existingOrder);
    }

    @Test
    void testCancelOrder_AlreadyCompleted() {
        Order existingOrder = new Order();
        existingOrder.setOrderId(1000);
        existingOrder.setOrderStatus(OrderStatus.Completed);

        when(orderDAO.findById(1000)).thenReturn(existingOrder);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.cancelOrder(1000));
        assertEquals("Cannot cancel a completed order", ex.getMessage());
    }

    @Test
    void testCancelOrder_NotFound() {
        when(orderDAO.findById(anyInt())).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.cancelOrder(999));
        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    void testCompleteOrder_Success() {
        Order existingOrder = new Order();
        existingOrder.setOrderId(1000);
        existingOrder.setOrderStatus(OrderStatus.Processing);

        when(orderDAO.findById(1000)).thenReturn(existingOrder);

        Order completed = orderService.completeOrder(1000);
        assertEquals(OrderStatus.Completed, completed.getOrderStatus());
        verify(orderDAO).update(existingOrder);
    }

    @Test
    void testCompleteOrder_Canceled() {
        Order existingOrder = new Order();
        existingOrder.setOrderId(1000);
        existingOrder.setOrderStatus(OrderStatus.Canceled);

        when(orderDAO.findById(1000)).thenReturn(existingOrder);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.completeOrder(1000));
        assertEquals("Cannot complete a canceled order", ex.getMessage());
    }
}