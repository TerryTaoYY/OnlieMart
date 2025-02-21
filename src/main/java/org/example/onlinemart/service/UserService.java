package org.example.onlinemart.service;

import org.example.onlinemart.entity.User;

import java.util.List;

public interface UserService {
    User register(User user);
    User findById(int userId);
    User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();
    /**
     * Update user fully or partially.
     * If a password is provided, re-hash it.
     */
    User updateUser(int userId, User updates);

    /**
     * Change the role of the user explicitly (e.g. promote/demote).
     */
    User changeUserRole(int userId, User.Role newRole);
}

