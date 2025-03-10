package org.example.onlinemart.service.impl;

import org.example.onlinemart.dao.UserDAO;
import org.example.onlinemart.dao.WatchlistDAO;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.Watchlist;
import org.example.onlinemart.service.UserService;
import org.example.onlinemart.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional 
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder passwordEncoder;

    private final WatchlistDAO watchlistDAO;

    public UserServiceImpl(UserDAO userDAO,
                           BCryptPasswordEncoder passwordEncoder,
                           WatchlistDAO watchlistDAO) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.watchlistDAO = watchlistDAO;
    }

    @Override
    public User register(User user) {
        if (userDAO.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists.");
        }
        if (userDAO.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email already exists.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userDAO.save(user);
        return user;
    }

    @Override
    public User findById(int userId) {
        return userDAO.findById(userId);
    }

    @Override
    public User findByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    @Override
    public User findByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userDAO.findAll();
    }

    @Override
    public User updateUser(int userId, User updates) {
        User existing = userDAO.findById(userId);
        if (existing == null) {
            throw new RuntimeException("User not found with ID " + userId);
        }
        if (updates.getUsername() != null && !updates.getUsername().equals(existing.getUsername())) {
            if (userDAO.findByUsername(updates.getUsername()) != null) {
                throw new RuntimeException("Username already taken: " + updates.getUsername());
            }
            existing.setUsername(updates.getUsername());
        }
        if (updates.getEmail() != null && !updates.getEmail().equals(existing.getEmail())) {
            if (userDAO.findByEmail(updates.getEmail()) != null) {
                throw new RuntimeException("Email already taken: " + updates.getEmail());
            }
            existing.setEmail(updates.getEmail());
        }
        if (updates.getPassword() != null && !updates.getPassword().trim().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(updates.getPassword()));
        }
        if (updates.getRole() != null) {
            existing.setRole(updates.getRole());
        }
        userDAO.update(existing);
        return existing;
    }

    @Override
    public User changeUserRole(int userId, User.Role newRole) {
        User existing = userDAO.findById(userId);
        if (existing == null) {
            throw new RuntimeException("User not found with ID " + userId);
        }
        existing.setRole(newRole);
        userDAO.update(existing);
        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getWatchlistProductsInStock(int userId) {
        List<Watchlist> watchlistItems = watchlistDAO.findByUserId(userId);
        return watchlistItems.stream()
                .map(Watchlist::getProduct)
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());
    }
}