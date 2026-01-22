package com.example.wq.util;

import com.example.wq.entity.AbstractHibernateBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Hibernate工具类
 */
@Component
public class HibernateUtils {

    @Autowired
    private EntityManager entityManager;

    /**
     * 通过反射创建实体实例并设置属性
     */
    public <T extends AbstractHibernateBean> T createEntityFromMap(Class<T> entityClass, Map<String, Object> data) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            try {
                Field field = entityClass.getDeclaredField(fieldName);
                field.setAccessible(true);

                // 类型转换
                Object convertedValue = convertValue(field.getType(), value);
                field.set(entity, convertedValue);
            } catch (NoSuchFieldException e) {
                // 忽略不存在的字段
                continue;
            }
        }

        return entity;
    }

    /**
     * 类型转换
     */
    public Object convertValue(Class<?> targetType, Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();

        if (targetType == String.class) {
            return stringValue;
        } else if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Integer) {
                return value;
            }
            return Integer.parseInt(stringValue);
        } else if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Long) {
                return value;
            }
            return Long.parseLong(stringValue);
        } else if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Double) {
                return value;
            }
            return Double.parseDouble(stringValue);
        } else if (targetType == Float.class || targetType == float.class) {
            if (value instanceof Float) {
                return value;
            }
            return Float.parseFloat(stringValue);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(stringValue);
        } else if (targetType == java.time.LocalDateTime.class) {
            // 支持多种日期时间格式
            if (value instanceof java.time.LocalDateTime) {
                return value;
            }

            // 尝试多种格式解析
            String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.S",
                "yyyy-MM-dd'T'HH:mm:ss.S",
                "yyyy-MM-dd"
            };

            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return java.time.LocalDateTime.parse(stringValue.replace("T", " "), formatter);
                } catch (Exception e) {
                    // 继续尝试下一个格式
                }
            }

            // 如果所有格式都失败，尝试 ISO 格式
            try {
                return java.time.LocalDateTime.parse(stringValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                throw new IllegalArgumentException("无法解析日期时间: " + stringValue);
            }
        }

        return value;
    }

    /**
     * 执行HQL查询
     */
    public <T> java.util.List<T> executeHqlQuery(String hql, Object[] params, Class<T> resultClass) {
        Query query = entityManager.createQuery(hql, resultClass);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }
        return query.getResultList();
    }

    /**
     * 执行原生SQL查询
     */
    public <T> java.util.List<T> executeNativeQuery(String sql, Object[] params, Class<T> resultClass) {
        Query query = entityManager.createNativeQuery(sql, resultClass);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }
        return query.getResultList();
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
     * 获取实体总数
     */
    public Long getCount(String hql, Object[] params) {
        Query query = entityManager.createQuery(hql, Long.class);
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
        }
        return (Long) query.getSingleResult();
    }
}
