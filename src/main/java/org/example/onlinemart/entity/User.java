package org.example.onlinemart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@AllArgsConstructor
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
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_USER;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updatedAt = new Date();

    public User() {}

    public User(String username, String email, String password, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = (role == null) ? Role.ROLE_USER : role;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }

    public boolean isAdmin() {
        return Role.ROLE_ADMIN.equals(this.role);
    }

    public boolean isUser() {
        return Role.ROLE_USER.equals(this.role);
    }

    public enum Role {
        ROLE_USER, ROLE_ADMIN
    }
}