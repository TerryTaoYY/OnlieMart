package org.example.onlinemart.service;

import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.User;

import java.util.List;

public interface UserService {
    User register(User user);
    User findById(int userId);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();

    User updateUser(int userId, User updates);
    User changeUserRole(int userId, User.Role newRole);

    List<Product> getWatchlistProductsInStock(int userId);
}

