package com.example.orderservice.service;

import com.example.orderservice.model.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {
    private final List<Order> orders = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong();

    public Order createOrder(Order order) {
        // Validate order total
        if (order.getQuantity() * order.getPrice() > 1000) {
            throw new IllegalArgumentException("Order total exceeds maximum limit of 1000");
        }

        // Set order ID and status
        order.setId(idCounter.incrementAndGet());
        order.setStatus("PENDING");

        // Apply discount for bulk orders
        if (order.getQuantity() >= 10) {
            order.setPrice(order.getPrice() * 0.9); // 10% discount
        }

        orders.add(order);
        return order;
    }

    public Order getOrder(Long id) {
        return orders.stream()
                .filter(order -> order.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    public List<Order> getOrdersByCustomer(String customerName) {
        List<Order> customerOrders = new ArrayList<>();
        for (Order order : orders) {
            if (order.getCustomerName().equalsIgnoreCase(customerName)) {
                customerOrders.add(order);
            }
        }
        return customerOrders;
    }

    public Order updateOrderStatus(Long id, String status) {
        Order order = getOrder(id);

        // Validate status transition
        if (order.getStatus().equals("CANCELLED") && !status.equals("CANCELLED")) {
            throw new IllegalStateException("Cannot update status of cancelled order");
        }

        order.setStatus(status);
        return order;
    }

    public double getTotalRevenue() {
        return orders.stream()
                .filter(order -> "COMPLETED".equals(order.getStatus()))
                .mapToDouble(order -> order.getPrice() * order.getQuantity())
                .sum();
    }
}