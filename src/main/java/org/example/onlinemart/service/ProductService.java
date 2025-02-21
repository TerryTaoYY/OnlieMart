package org.example.onlinemart.service;

import org.example.onlinemart.entity.Product;

import java.util.List;

public interface ProductService {

    Product save(Product product);

    Product updateProductFields(int productId, Product updates);

    Product findById(int productId);

    List<Product> findAll();

    List<Product> findAllInStock();
}
