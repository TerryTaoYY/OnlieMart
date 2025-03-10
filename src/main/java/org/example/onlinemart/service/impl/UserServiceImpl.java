package org.example.onlinemart.service.impl;

import org.example.onlinemart.cache.CacheKeys;
import org.example.onlinemart.cache.CacheService;
import org.example.onlinemart.dao.UserDAO;
import org.example.onlinemart.dao.WatchlistDAO;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.Watchlist;
import org.example.onlinemart.service.UserService;
import org.example.onlinemart.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WatchlistDAO watchlistDAO;
    private final CacheService cacheService;

    @Value("${redis.cache.users.TTL:300}")
    private long userCacheTTL;

    @Value("${redis.cache.watchlist.TTL:120}")
    private long watchlistCacheTTL;

    @Autowired
    public UserServiceImpl(UserDAO userDAO,
                           BCryptPasswordEncoder passwordEncoder,
                           WatchlistDAO watchlistDAO,
                           CacheService cacheService) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
        this.watchlistDAO = watchlistDAO;
        this.cacheService = cacheService;
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

        // Invalidate the all users cache
        cacheService.delete(CacheKeys.Users.ALL);

        return user;
    }

    @Override
    public User findById(int userId) {
        String cacheKey = CacheKeys.Users.user(userId);
        Optional<User> cachedUser = cacheService.get(cacheKey, User.class);

        if (cachedUser.isPresent()) {
            logger.debug("Cache hit for user ID: {}", userId);
            return cachedUser.get();
        }

        logger.debug("Cache miss for user ID: {}", userId);
        User user = userDAO.findById(userId);

        if (user != null) {
            // Cache the user
            cacheService.set(cacheKey, user, userCacheTTL, TimeUnit.SECONDS);
        }

        return user;
    }

    @Override
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String cacheKey = CacheKeys.Users.username(username);
        Optional<User> cachedUser = cacheService.get(cacheKey, User.class);

        if (cachedUser.isPresent()) {
            logger.debug("Cache hit for username: {}", username);
            return cachedUser.get();
        }

        logger.debug("Cache miss for username: {}", username);
        User user = userDAO.findByUsername(username);

        if (user != null) {
            // Cache the user
            cacheService.set(cacheKey, user, userCacheTTL, TimeUnit.SECONDS);

            // Also update the cache by userId to avoid inconsistency
            cacheService.set(CacheKeys.Users.user(user.getUserId()), user, userCacheTTL, TimeUnit.SECONDS);
        }

        return user;
    }

    @Override
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String cacheKey = CacheKeys.Users.email(email);
        Optional<User> cachedUser = cacheService.get(cacheKey, User.class);

        if (cachedUser.isPresent()) {
            logger.debug("Cache hit for email: {}", email);
            return cachedUser.get();
        }

        logger.debug("Cache miss for email: {}", email);
        User user = userDAO.findByEmail(email);

        if (user != null) {
            // Cache the user
            cacheService.set(cacheKey, user, userCacheTTL, TimeUnit.SECONDS);

            // Also update the cache by userId to avoid inconsistency
            cacheService.set(CacheKeys.Users.user(user.getUserId()), user, userCacheTTL, TimeUnit.SECONDS);
        }

        return user;
    }

    @Override
    public List<User> findAll() {
        String cacheKey = CacheKeys.Users.ALL;
        Optional<List<User>> cachedUsers = cacheService.getList(cacheKey, User.class);

        if (cachedUsers.isPresent()) {
            logger.debug("Cache hit for all users");
            return cachedUsers.get();
        }

        logger.debug("Cache miss for all users");
        List<User> users = userDAO.findAll();

        // Cache the result
        cacheService.set(cacheKey, users, userCacheTTL, TimeUnit.SECONDS);

        return users;
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

        // Invalidate all user-related caches
        invalidateUserCaches(existing);

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

        // Invalidate all user-related caches
        invalidateUserCaches(existing);

        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getWatchlistProductsInStock(int userId) {
        String cacheKey = CacheKeys.Users.watchlist(userId);
        Optional<List<Product>> cachedProducts = cacheService.getList(cacheKey, Product.class);

        if (cachedProducts.isPresent()) {
            logger.debug("Cache hit for user watchlist: {}", userId);
            return cachedProducts.get();
        }

        logger.debug("Cache miss for user watchlist: {}", userId);
        List<Watchlist> watchlistItems = watchlistDAO.findByUserId(userId);

        List<Product> products = watchlistItems.stream()
                .map(Watchlist::getProduct)
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());

        // Cache the result with a shorter TTL since stock may change
        cacheService.set(cacheKey, products, watchlistCacheTTL, TimeUnit.SECONDS);

        return products;
    }

    /**
     * Helper method to invalidate all caches related to a user
     *
     * @param user The user whose caches should be invalidated
     */
    private void invalidateUserCaches(User user) {
        try {
            int userId = user.getUserId();

            // Delete direct user caches
            cacheService.delete(CacheKeys.Users.user(userId));
            cacheService.delete(CacheKeys.Users.username(user.getUsername()));
            cacheService.delete(CacheKeys.Users.email(user.getEmail()));
            cacheService.delete(CacheKeys.Users.watchlist(userId));
            cacheService.delete(CacheKeys.Users.ALL);

            // Delete activity caches
            cacheService.delete(CacheKeys.UserActivity.frequentPurchases(userId, 3));
            cacheService.delete(CacheKeys.UserActivity.recentPurchases(userId, 3));

            // Delete session caches if role changed (for security)
            cacheService.delete(CacheKeys.Session.userActiveSessions(userId));

            logger.debug("Successfully invalidated caches for user ID: {}", userId);
        } catch (Exception e) {
            // Log but don't rethrow - cache invalidation failure shouldn't break core functionality
            logger.warn("Failed to invalidate user caches", e);
        }
    }
}