package com.example.wq.repository;

import com.example.wq.entity.AdminUser;
import com.example.wq.enums.DeletedFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员数据访问层
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {

    /**
     * 根据用户名和删除标记查询管理员
     *
     * @param username 用户名
     * @param deleted  删除标记
     * @return 管理员信息
     */
    Optional<AdminUser> findByUsernameAndDeleted(String username, Integer deleted);

    /**
     * 根据用户名查询管理员（包含已删除）
     *
     * @param username 用户名
     * @return 管理员信息
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
}
