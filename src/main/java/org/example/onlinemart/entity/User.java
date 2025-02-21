package org.example.onlinemart.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password; // hashed password

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_USER; // default

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updatedAt = new Date();

    // Constructors, getters, setters ...

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    public enum Role {
        ROLE_USER, ROLE_ADMIN
    }

    // getters, setters, etc.
}
