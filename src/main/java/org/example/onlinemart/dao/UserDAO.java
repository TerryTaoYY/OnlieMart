package org.example.onlinemart.dao;

import org.example.onlinemart.entity.User;

import java.util.List;

public interface UserDAO {
    void save(User user);
    void update(User user);
    User findById(int userId);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();
}
