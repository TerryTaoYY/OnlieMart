package org.example.onlinemart.dao;

import org.example.onlinemart.entity.Product;

import java.util.List;

public interface ProductDAO {
    void save(Product product);
    void update(Product product);
    Product findById(int productId);
    List<Product> findAll();
    List<Product> findAllInStock();
}
