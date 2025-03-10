package org.example.onlinemart.service.impl;

import org.example.onlinemart.cache.CacheKeys;
import org.example.onlinemart.cache.CacheService;
import org.example.onlinemart.dao.ProductDAO;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductDAO productDAO;
    private final CacheService cacheService;

    @Value("${redis.cache.products.TTL:600}")
    private long productCacheTTL;

    @Autowired
    public ProductServiceImpl(ProductDAO productDAO, CacheService cacheService) {
        this.productDAO = productDAO;
        this.cacheService = cacheService;
    }

    @Override
    public Product save(Product product) {
        product.setCreatedAt(new Date());
        product.setUpdatedAt(new Date());
        productDAO.save(product);

        // Invalidate product-related caches
        invalidateProductCaches();

        return product;
    }

    @Override
    public Product updateProductFields(int productId, Product updates) {
        Product existing = productDAO.findById(productId);
        if (existing == null) {
            throw new RuntimeException("Product with ID " + productId + " not found");
        }

        boolean stockChanged = false;

        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        if (updates.getRetailPrice() != 0) {
            existing.setRetailPrice(updates.getRetailPrice());
        }
        if (updates.getWholesalePrice() != 0) {
            existing.setWholesalePrice(updates.getWholesalePrice());
        }
        if (updates.getStock() != 0) {
            stockChanged = existing.getStock() != updates.getStock();
            existing.setStock(updates.getStock());
        }
        if (updates.getProductName() != null) {
            existing.setProductName(updates.getProductName());
        }

        existing.setUpdatedAt(new Date());
        productDAO.update(existing);

        // Invalidate product caches
        String productKey = CacheKeys.Products.product(productId);
        cacheService.delete(productKey);

        // If stock changed, also invalidate the IN_STOCK cache
        if (stockChanged) {
            cacheService.delete(CacheKeys.Products.IN_STOCK);
        }

        // Always invalidate the ALL products cache
        cacheService.delete(CacheKeys.Products.ALL);

        return existing;
    }

    @Override
    public Product findById(int productId) {
        String cacheKey = CacheKeys.Products.product(productId);
        Optional<Product> cachedProduct = cacheService.get(cacheKey, Product.class);

        if (cachedProduct.isPresent()) {
            logger.debug("Cache hit for product ID: {}", productId);
            return cachedProduct.get();
        }

        logger.debug("Cache miss for product ID: {}", productId);
        Product product = productDAO.findById(productId);

        if (product != null) {
            // Cache the product
            cacheService.set(cacheKey, product, productCacheTTL, TimeUnit.SECONDS);
        }

        return product;
    }

    @Override
    public List<Product> findAll() {
        String cacheKey = CacheKeys.Products.ALL;
        Optional<List<Product>> cachedProducts = cacheService.getList(cacheKey, Product.class);

        if (cachedProducts.isPresent()) {
            logger.debug("Cache hit for all products");
            return cachedProducts.get();
        }

        logger.debug("Cache miss for all products");
        List<Product> products = productDAO.findAll();

        // Cache the result
        cacheService.set(cacheKey, products, productCacheTTL, TimeUnit.SECONDS);

        return products;
    }

    @Override
    public List<Product> findAllInStock() {
        String cacheKey = CacheKeys.Products.IN_STOCK;
        Optional<List<Product>> cachedProducts = cacheService.getList(cacheKey, Product.class);

        if (cachedProducts.isPresent()) {
            logger.debug("Cache hit for in-stock products");
            return cachedProducts.get();
        }

        logger.debug("Cache miss for in-stock products");
        List<Product> products = productDAO.findAllInStock();

        // Cache the result
        cacheService.set(cacheKey, products, productCacheTTL, TimeUnit.SECONDS);

        return products;
    }

    /**
     * Helper method to invalidate all product-related caches
     */
    private void invalidateProductCaches() {
        try {
            cacheService.delete(CacheKeys.Products.ALL);
            cacheService.delete(CacheKeys.Products.IN_STOCK);

            // Also invalidate admin summary caches that depend on products
            cacheService.delete(CacheKeys.AdminSummary.MOST_PROFITABLE);
            cacheService.delete(CacheKeys.AdminSummary.topPopular(3));
        } catch (Exception e) {
            // Log but don't rethrow - cache invalidation failure shouldn't break core functionality
            logger.warn("Failed to invalidate product caches", e);
        }
    }
}