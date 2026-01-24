package com.example.wq.repository;

import com.example.wq.entity.WqUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * WqUser Repository
 */
@Repository
public interface WqUserRepository extends JpaRepository<WqUser, String> {

    /**
     * 根据 OpenID 查询用户
     *
     * @param openid 微信OpenID
     * @return 用户信息
     */
    Optional<WqUser> findByOpenid(String openid);

    /**
     * 根据 UnionID 查询用户
     *
     * @param unionid 微信UnionID
     * @return 用户信息
     */
    Optional<WqUser> findByUnionid(String unionid);

    /**
     * 根据 OpenID 查询用户（排除已删除的）
     *
     * @param openid 微信OpenID
     * @return 用户信息
     */
    Optional<WqUser> findByOpenidAndDeleted(String openid, Integer deleted);

    /**
     * 根据 UnionID 查询用户（排除已删除的）
     *
     * @param unionid 微信UnionID
     * @param deleted 删除标记
     * @return 用户信息
     */
    Optional<WqUser> findByUnionidAndDeleted(String unionid, Integer deleted);

    /**
     * 检查 OpenID 是否存在
     *
     * @param openid 微信OpenID
     * @return 是否存在
     */
    boolean existsByOpenid(String openid);
}
