package org.example.onlinemart.entity;

import javax.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    // Snapshots to preserve pricing at the time of purchase
    @Column(nullable = false, precision = 10, scale = 2)
    private double wholesalePriceSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private double retailPriceSnapshot;

    // getters, setters ...
}
