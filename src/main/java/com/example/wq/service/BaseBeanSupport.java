package com.example.wq.service;

import com.example.wq.entity.AbstractHibernateBean;
import com.example.wq.repository.EtlDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基础Bean支持服务 - 提供标准化的CRUD操作
 *
 * 注意：此类不使用类级别事务，具体方法根据需要在调用层添加事务
 */
@Service
public class BaseBeanSupport {

    @Autowired
    private EtlDao edao;

    /**
     * 创建实体
     */
    public <Bean extends AbstractHibernateBean> Bean create(Bean bean) {
        // 自动生成ID
        if (bean.get_id() == null || bean.get_id().isEmpty()) {
            bean.set_id(AbstractHibernateBean.generateId());
        }

        // 保存并刷新
        edao.save(bean);
        edao.flush();
        edao.clear();

        // 重新加载（确保懒加载数据）
        Bean result = (Bean) edao.findById(bean.getClass(), bean.get_id());
        return result != null ? result : bean;
    }

    /**
     * 更新实体
     */
    public <Bean extends AbstractHibernateBean> Bean update(Bean bean) {
        Bean updated = edao.update(bean);
        edao.flush();
        return updated;
    }

    /**
     * 根据ID查询实体
     */
    public <Bean extends AbstractHibernateBean> Bean findById(Class<Bean> beanClass, String id) {
        return edao.findById(beanClass, id);
    }

    /**
     * 删除实体
     */
    public <Bean extends AbstractHibernateBean> void delete(Bean bean) {
        edao.delete(bean);
        edao.flush();
    }

    /**
     * 根据ID删除实体
     */
    public <Bean extends AbstractHibernateBean> void deleteById(Class<Bean> beanClass, String id) {
        edao.deleteById(beanClass, id);
        edao.flush();
    }

    /**
     * 执行HQL查询
     */
    public <T> java.util.List<T> executeHql(String hql, Object[] params) {
        return edao.getList(hql, params);
    }

    /**
     * 执行分页查询
     */
    public <T> java.util.List<T> executeHqlPage(String hql, int pageNum, int pageSize, Object[] params) {
        return edao.getPage(hql, pageNum, pageSize, params);
    }

    /**
     * 获取总数
     */
    public Long getCount(String countHql, Object[] params) {
        return edao.getCount(countHql, params);
    }

    /**
     * 执行更新操作
     */
    public int executeUpdate(String hql, Object[] params) {
        return edao.executeUpdate(hql, params);
    }
}

