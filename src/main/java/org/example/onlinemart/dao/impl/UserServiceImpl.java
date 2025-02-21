package org.example.onlinemart.dao.impl;

import org.example.onlinemart.dao.UserDAO;
import org.example.onlinemart.dao.UserService;
import org.example.onlinemart.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserDAO userDAO, BCryptPasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(User user) {
        // check duplicates
        if (userDAO.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists.");
        }
        if (userDAO.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email already exists.");
        }
        // encode password
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
    public void update(User user) {
        userDAO.update(user);
    }
}
