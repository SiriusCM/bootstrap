package com.sirius.bootstrap.module.order;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体类
 */
public class Order {
    // 订单ID
    private String orderId;
    // 订单金额
    private BigDecimal amount;
    // 客户等级
    private String customerLevel;
    // 订单日期
    private Date orderDate;
    // 折扣金额
    private BigDecimal discount;
    // 最终支付金额
    private BigDecimal payAmount;

    // 构造函数、getter和setter
    public Order(String orderId, BigDecimal amount, String customerLevel, Date orderDate) {
        this.orderId = orderId;
        this.amount = amount;
        this.customerLevel = customerLevel;
        this.orderDate = orderDate;
    }

    // getter和setter方法
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCustomerLevel() {
        return customerLevel;
    }

    public void setCustomerLevel(String customerLevel) {
        this.customerLevel = customerLevel;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", amount=" + amount +
                ", customerLevel='" + customerLevel + '\'' +
                ", discount=" + discount +
                ", payAmount=" + payAmount +
                '}';
    }
}
