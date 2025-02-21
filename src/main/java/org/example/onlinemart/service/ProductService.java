package org.example.onlinemart.service;

import org.example.onlinemart.entity.Product;

import java.util.List;

public interface ProductService {
    /**
     * Create/save a new product in the DB.
     */
    Product save(Product product);

    /**
     * Partially update fields of an existing product,
     * or fully replace them depending on your logic.
     */
    Product updateProductFields(int productId, Product updates);

    /**
     * Find a product by its ID.
     */
    Product findById(int productId);

    /**
     * Get all products, regardless of stock level.
     */
    List<Product> findAll();

    /**
     * Get only in-stock products (stock > 0).
     */
    List<Product> findAllInStock();
}
