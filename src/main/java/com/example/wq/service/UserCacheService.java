package com.example.wq.service;

import com.example.wq.entity.WqUser;
import com.example.wq.repository.EtlDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户缓存服务 - 展示 Spring Cache 注解的使用
 */
@Service
public class UserCacheService {

    @Autowired
    private EtlDao edao;

    /**
     * 查询用户 - 使用缓存
     *
     * cacheNames: 缓存名称
     * key: 缓存键（SpEL表达式，#id表示方法参数）
     * unless: 条件（结果为null时不缓存）
     */
    @Cacheable(
        cacheNames = "userCache",
        key = "'user:' + #id",
        unless = "#result == null"
    )
    public WqUser getUserById(String id) {
        System.out.println("从数据库查询用户: " + id);
        // 模拟数据库查询
        return (WqUser) edao.findById(WqUser.class, id);
    }

    /**
     * 查询用户 - 使用缓存（更复杂的键）
     */
    @Cacheable(
        cacheNames = "userCache",
        key = "'user:username:' + #username",
        unless = "#result == null"
    )
    public WqUser getUserByUsername(String username) {
        System.out.println("从数据库查询用户: " + username);
        // 实际应该通过 username 查询
        return null;
    }

    /**
     * 更新用户 - 更新缓存
     *
     * CachePut: 每次都执行方法，并更新缓存
     */
    @CachePut(
        cacheNames = "userCache",
        key = "'user:' + #user._id"
    )
    public WqUser updateUser(WqUser user) {
        System.out.println("更新用户并刷新缓存: " + user.get_id());
        // 更新数据库
        edao.update(user);
        return user;
    }

    /**
     * 删除用户 - 清除缓存
     *
     * CacheEvict: 删除缓存
     * allEntries: true 表示清空整个缓存区域
     */
    @CacheEvict(
        cacheNames = "userCache",
        key = "'user:' + #id"
    )
    public void deleteUser(String id) {
        System.out.println("删除用户并清除缓存: " + id);
        // 删除数据库
        edao.deleteById(WqUser.class, id);
    }

    /**
     * 批量删除用户 - 清除所有缓存
     */
    @CacheEvict(
        cacheNames = "userCache",
        allEntries = true
    )
    public void deleteAllUsers() {
        System.out.println("清空用户缓存");
        // 删除所有用户
    }

    /**
     * 创建用户 - 不使用缓存
     *
     * 插入操作通常不需要缓存，因为查询时会自动缓存
     */
    public WqUser createUser(Map<String, Object> userData) {
        System.out.println("创建新用户");
        // 创建用户
        WqUser user = new WqUser();
        // ... 设置属性
        return user;
    }

    /**
     * 条件缓存 - 根据条件决定是否缓存
     *
     * condition: 方法执行前判断（不满足条件则不执行方法）
     * unless: 方法执行后判断（结果满足条件则不缓存）
     */
    @Cacheable(
        cacheNames = "userCache",
        key = "'user:' + #id",
        condition = "#id != null && #id.length() > 0",  // id 不为空才执行方法
        unless = "#result == null || #result.status != 1"  // 只缓存状态正常的用户
    )
    public WqUser getActiveUser(String id) {
        System.out.println("查询活跃用户: " + id);
        return (WqUser) edao.findById(WqUser.class, id);
    }
}
