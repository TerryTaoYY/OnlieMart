package org.example.onlinemart.service.impl;

import lombok.*;
import org.example.onlinemart.dao.UserDAO;
import org.example.onlinemart.dao.WatchlistDAO;
import org.example.onlinemart.entity.User;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.Watchlist;
import org.example.onlinemart.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private WatchlistDAO watchlistDAO;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1);
        sampleUser.setUsername("john_doe");
        sampleUser.setEmail("john@example.com");
        sampleUser.setPassword("plaintext");
    }

    @Test
    void testRegister_Success() {
        when(userDAO.findByUsername("john_doe")).thenReturn(null);
        when(userDAO.findByEmail("john@example.com")).thenReturn(null);
        when(passwordEncoder.encode("plaintext")).thenReturn("hashed");
        doNothing().when(userDAO).save(any(User.class));

        User newUser = userService.register(sampleUser);
        assertNotNull(newUser);
        assertEquals("hashed", newUser.getPassword());
        verify(userDAO, times(1)).save(newUser);
    }

    @Test
    void testRegister_UserExists() {
        when(userDAO.findByUsername("john_doe")).thenReturn(new User());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(sampleUser));
        assertEquals("Username already exists.", ex.getMessage());
    }

    @Test
    void testRegister_EmailExists() {
        when(userDAO.findByUsername("john_doe")).thenReturn(null);
        when(userDAO.findByEmail("john@example.com")).thenReturn(new User());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(sampleUser));
        assertEquals("Email already exists.", ex.getMessage());
    }

    @Test
    void testUpdateUser_Success() {
        when(userDAO.findById(1)).thenReturn(sampleUser);
        when(userDAO.findByUsername("new_user")).thenReturn(null);
        when(userDAO.findByEmail("new@example.com")).thenReturn(null);
        when(passwordEncoder.encode("newpass")).thenReturn("hashedNew");

        User updates = new User();
        updates.setUsername("new_user");
        updates.setEmail("new@example.com");
        updates.setPassword("newpass");

        userService.updateUser(1, updates);

        assertEquals("new_user", sampleUser.getUsername());
        assertEquals("new@example.com", sampleUser.getEmail());
        assertEquals("hashedNew", sampleUser.getPassword());
        verify(userDAO, times(1)).update(sampleUser);
    }

    @Test
    void testUpdateUser_NotFound() {
        when(userDAO.findById(999)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateUser(999, new User()));
        assertEquals("User not found with ID 999", ex.getMessage());
    }

    @Test
    void testGetWatchlistProductsInStock() {
        Product p1 = new Product(); p1.setProductId(10); p1.setStock(5);
        Product p2 = new Product(); p2.setProductId(20); p2.setStock(0);  // out of stock
        Watchlist w1 = new Watchlist(); w1.setProduct(p1);
        Watchlist w2 = new Watchlist(); w2.setProduct(p2);

        when(watchlistDAO.findByUserId(1)).thenReturn(Arrays.asList(w1, w2));

        var products = userService.getWatchlistProductsInStock(1);
        assertEquals(1, products.size());
        assertEquals(10, products.get(0).getProductId());
        verify(watchlistDAO, times(1)).findByUserId(1);
    }
}