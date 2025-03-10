package org.example.onlinemart.service.impl;

import org.example.onlinemart.dao.ProductDAO;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductDAO productDAO;

    public ProductServiceImpl(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    @Override
    public Product save(Product product) {
        product.setCreatedAt(new Date());
        product.setUpdatedAt(new Date());
        productDAO.save(product);
        return product;
    }

    @Override
    public Product updateProductFields(int productId, Product updates) {
        Product existing = productDAO.findById(productId);
        if (existing == null) {
            throw new RuntimeException("Product with ID " + productId + " not found");
        }
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
            existing.setStock(updates.getStock());
        }
        if (updates.getProductName() != null) {
            existing.setProductName(updates.getProductName());
        }

        existing.setUpdatedAt(new Date());
        productDAO.update(existing);
        return existing;
    }

    @Override
    public Product findById(int productId) {
        return productDAO.findById(productId);
    }

    @Override
    public List<Product> findAll() {
        return productDAO.findAll();
    }

    @Override
    public List<Product> findAllInStock() {
        return productDAO.findAllInStock();
    }
}