package com.example.wq.repository;

import com.example.wq.entity.AbstractHibernateBean;
import com.example.wq.util.HibernateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 数据访问对象
 *
 * 注意：DAO层不应该管理事务，事务由业务服务层统一管理
 */
@Repository
public class EtlDao {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private HibernateUtils hibernateUtils;

    /**
     * 保存实体
     */
    public <T extends AbstractHibernateBean> void save(T entity) {
        if (entity.get_id() == null || entity.get_id().isEmpty()) {
            entity.set_id(AbstractHibernateBean.generateId());
        }
        entityManager.persist(entity);
    }

    /**
     * 更新实体
     */
    public <T extends AbstractHibernateBean> T update(T entity) {
        return entityManager.merge(entity);
    }

    /**
     * 根据ID查找实体
     */
    public <T extends AbstractHibernateBean> T findById(Class<T> entityClass, String id) {
        return entityManager.find(entityClass, id);
    }

    /**
     * 删除实体
     */
    public <T extends AbstractHibernateBean> void delete(T entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    /**
     * 根据ID删除实体
     */
    public <T extends AbstractHibernateBean> void deleteById(Class<T> entityClass, String id) {
        T entity = findById(entityClass, id);
        if (entity != null) {
            delete(entity);
        }
    }

    /**
     * 执行HQL查询，返回列表
     */
    public <T> List<T> getList(String hql, Object[] params) {
        Query query = entityManager.createQuery(hql);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }
        return query.getResultList();
    }

    /**
     * 执行分页查询
     */
    public <T> List<T> getPage(String hql, int pageNum, int pageSize, Object[] params) {
        Query query = entityManager.createQuery(hql);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }
        query.setFirstResult((pageNum - 1) * pageSize);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    /**
     * 获取总数
     */
    public Long getCount(String countHql, Object[] params) {
        Query query = entityManager.createQuery(countHql, Long.class);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }
        return (Long) query.getSingleResult();
    }

    /**
     * 执行更新操作
     */
    public int executeUpdate(String hql, Object[] params) {
        Query query = entityManager.createQuery(hql);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }
        return query.executeUpdate();
    }

    /**
     * 刷新session
     */
    public void flush() {
        entityManager.flush();
    }

    /**
     * 清除session缓存
     */
    public void clear() {
        entityManager.clear();
    }

    /**
     * 创建分页对象
     */
    public <T> Page<T> createPage(List<T> content, Pageable pageable, Long total) {
        return new PageImpl<>(content, pageable, total);
    }
}

