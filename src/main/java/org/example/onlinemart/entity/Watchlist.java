package org.example.onlinemart.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "watchlist",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "product_id"})
        })
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int watchlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt = new Date();

    // getters, setters ...
}