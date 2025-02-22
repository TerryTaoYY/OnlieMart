package org.example.onlinemart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private int orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

//    @Column(name = "wholesale_price_snapshot", nullable = false, precision = 10, scale = 2)
//    private BigDecimal wholesalePriceSnapshot;
//
//    @Column(name = "retail_price_snapshot", nullable = false, precision = 10, scale = 2)
//    private BigDecimal retailPriceSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private double wholesalePriceSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private double retailPriceSnapshot;

}
