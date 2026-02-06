package com.example.wq.repository;

import com.example.wq.entity.UserAddress;
import com.example.wq.enums.YesNo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户收货地址 Repository
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, String> {

    /**
     * 查询用户的所有地址（按默认地址优先，然后按使用次数降序）
     */
    @Query("SELECT a FROM UserAddress a WHERE a.userId = :userId ORDER BY a.isDefault DESC, a.usedCount DESC")
    List<UserAddress> findByUserIdOrderByIsDefaultDescUsedCountDesc(@Param("userId") String userId);

    /**
     * 查询用户的默认地址
     */
    Optional<UserAddress> findByUserIdAndIsDefault(String userId, Integer isDefault);

    /**
     * 统计用户地址数量
     */
    long countByUserId(String userId);

    /**
     * 取消用户的所有默认地址
     */
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = :defaultValue WHERE a.userId = :userId")
    int cancelAllDefaultByUserId(@Param("userId") String userId, @Param("defaultValue") Integer defaultValue);
}
