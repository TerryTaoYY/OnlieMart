package org.example.onlinemart.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private double wholesalePrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private double retailPrice;

    @Column(nullable = false)
    private int stock;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updatedAt = new Date();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    // getters, setters ...
}
