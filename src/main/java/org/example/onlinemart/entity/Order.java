package org.example.onlinemart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus = OrderStatus.Processing;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date orderTime = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updatedAt = new Date();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    public enum OrderStatus {
        Processing, Canceled, Completed
    }
}
