package com.example.wq.repository;

import com.example.wq.entity.InventoryLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 库存预占 Repository
 */
@Repository
public interface InventoryLockRepository extends JpaRepository<InventoryLock, String> {

    /**
     * 查询商品的有效预占记录（未过期且已锁定）
     */
    @Query("SELECT SUM(il.quantity) FROM InventoryLock il WHERE il.productId = :productId AND il.status = 1 AND il.expireTime > :currentTime")
    Integer sumLockedQuantity(@Param("productId") String productId, @Param("currentTime") Long currentTime);

    /**
     * 释放订单的所有库存预占
     */
    @Modifying
    @Query("UPDATE InventoryLock il SET il.status = 0 WHERE il.orderId = :orderId")
    int releaseByOrderId(@Param("orderId") String orderId);

    /**
     * 查询过期的库存预占记录
     */
    @Query("SELECT il FROM InventoryLock il WHERE il.expireTime <= :currentTime AND il.status = 1")
    List<InventoryLock> findExpiredLocks(@Param("currentTime") Long currentTime);
}
