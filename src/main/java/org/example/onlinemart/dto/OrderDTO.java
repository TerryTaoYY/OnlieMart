package org.example.onlinemart.dto;

import org.example.onlinemart.entity.Order;

import java.util.Date;

public class OrderDTO {
    private int orderId;
    private UserDTO user;
    private String orderStatus;
    private Date orderTime;
    private Date updatedAt;

    public OrderDTO() {
    }

    public static OrderDTO fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUser(UserDTO.fromEntity(order.getUser()));
        dto.setOrderStatus(order.getOrderStatus().name());
        dto.setOrderTime(order.getOrderTime());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    public int getOrderId() {
        return orderId;
    }
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }
    public UserDTO getUser() {
        return user;
    }
    public void setUser(UserDTO user) {
        this.user = user;
    }
    public String getOrderStatus() {
        return orderStatus;
    }
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    public Date getOrderTime() {
        return orderTime;
    }
    public void setOrderTime(Date orderTime) {
        this.orderTime = orderTime;
    }
    public Date getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}