package com.om.To_Do.List.ecosystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@Table(name = "subscriptions")
public class Subscription {

    @Id
    private Long userId;

    private String subscriptionId;   // Razorpay subscription ID

    private LocalDate startDate;
    private LocalDate expiryDate;
    private Boolean isActive;

    // New fields for failure tracking:
    private Integer failureCount = 0;
    private LocalDateTime lastFailureAt;

    public Subscription() {
    }

    public Subscription(Long userId, String subscriptionId, LocalDate startDate, LocalDate expiryDate, Boolean isActive, Integer failureCount, LocalDateTime lastFailureAt) {
        this.userId = userId;
        this.subscriptionId = subscriptionId;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
        this.failureCount = failureCount;
        this.lastFailureAt = lastFailureAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getLastFailureAt() {
        return lastFailureAt;
    }

    public void setLastFailureAt(LocalDateTime lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
    }
}

