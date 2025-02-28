package org.example.onlinemart.service.impl;

import org.example.onlinemart.dao.ProductDAO;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductDAO productDAO;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setProductId(1);
        sampleProduct.setProductName("Test Product");
        sampleProduct.setStock(20);
        sampleProduct.setRetailPrice(29.99);
    }

    @Test
    void testSave_Success() {
        doAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(99);
            return null;
        }).when(productDAO).save(any(Product.class));

        Product saved = productService.save(sampleProduct);

        assertNotNull(saved);
        assertEquals(99, saved.getProductId());
        verify(productDAO, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProductFields_Success() {
        Product existing = new Product();
        existing.setProductId(1);
        existing.setStock(10);
        existing.setRetailPrice(50.0);

        when(productDAO.findById(1)).thenReturn(existing);
        doNothing().when(productDAO).update(any(Product.class));

        Product updates = new Product();
        updates.setStock(100);
        updates.setRetailPrice(59.99);

        Product updated = productService.updateProductFields(1, updates);
        assertEquals(100, updated.getStock());
        assertEquals(59.99, updated.getRetailPrice());
        verify(productDAO, times(1)).update(existing);
    }

    @Test
    void testUpdateProductFields_NotFound() {
        when(productDAO.findById(999)).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productService.updateProductFields(999, new Product()));
        assertEquals("Product with ID 999 not found", ex.getMessage());
    }

    @Test
    void testFindById_Success() {
        when(productDAO.findById(1)).thenReturn(sampleProduct);

        Product found = productService.findById(1);
        assertNotNull(found);
        assertEquals(1, found.getProductId());
        verify(productDAO, times(1)).findById(1);
    }

    @Test
    void testFindAll_Empty() {
        when(productDAO.findAll()).thenReturn(Collections.emptyList());

        List<Product> all = productService.findAll();
        assertTrue(all.isEmpty());
        verify(productDAO, times(1)).findAll();
    }
}