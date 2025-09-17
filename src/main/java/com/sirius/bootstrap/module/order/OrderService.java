package com.sirius.bootstrap.module.order;

import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * 订单服务类，使用Drools计算折扣
 */
@Service
public class OrderService {

    private final KieSession kieSession;

    @Autowired
    public OrderService(KieSession kieSession) {
        this.kieSession = kieSession;
    }

    /**
     * 计算订单折扣
     */
    public Order calculateDiscount(Order order) {
        // 将订单对象插入规则引擎
        kieSession.insert(order);
        // 执行所有匹配的规则
        int fireCount = kieSession.fireAllRules();
        System.out.println("执行了 " + fireCount + " 条规则");

        // 计算最终支付金额
        order.setPayAmount(order.getAmount().subtract(order.getDiscount()));
        return order;
    }

    @Bean
    public CommandLineRunner demoOrder(OrderService orderService) {
        return args -> {
            System.out.println("=== Drools订单折扣计算演示 ===");

            // 创建测试订单1：VIP客户，金额1500元
            Order order1 = new Order("ORDER001", new BigDecimal("1500"), "VIP", new Date());
            Order result1 = orderService.calculateDiscount(order1);
            System.out.println("订单1结果: " + result1);

            // 创建测试订单2：普通客户，金额2500元
            Order order2 = new Order("ORDER002", new BigDecimal("2500"), "NORMAL", new Date());
            Order result2 = orderService.calculateDiscount(order2);
            System.out.println("订单2结果: " + result2);

            // 创建测试订单3：普通客户，金额800元，节假日
            Calendar calendar = Calendar.getInstance();
            calendar.set(2024, Calendar.DECEMBER, 25); // 圣诞节
            Order order3 = new Order("ORDER003", new BigDecimal("800"), "NORMAL", calendar.getTime());
            Order result3 = orderService.calculateDiscount(order3);
            System.out.println("订单3结果: " + result3);
        };
    }
}
