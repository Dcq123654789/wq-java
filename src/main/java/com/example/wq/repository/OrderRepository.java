package com.example.wq.repository;

import com.example.wq.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单 Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    /**
     * 根据订单编号查询
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 查询用户的订单
     */
    List<Order> findByUserIdOrderByCreateTimeDesc(String userId);

    /**
     * 查询超时未支付的订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = 0 AND o.createTime < :expireTime")
    List<Order> findExpiredPendingOrders(@Param("expireTime") LocalDateTime expireTime);
}
