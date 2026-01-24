package com.example.wq.service;

import com.example.wq.entity.AbstractHibernateBean;
import com.example.wq.entity.Community;
import com.example.wq.entity.HqlQuery;
import com.example.wq.entity.PageResult;
import com.example.wq.entity.Result;
import com.example.wq.entity.WqUser;
import com.example.wq.repository.EtlDao;
import com.example.wq.util.HibernateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务 - 核心业务逻辑层
 */
@Service
public class UserService {

    @Autowired
    private EtlDao edao;

    @Autowired
    private BaseBeanSupport baseBeanSupport;

    @Autowired
    private HibernateUtils hibernateUtils;

    // 实体映射
    private Map<String, Class<?>> entityMap = new HashMap<>();

    public UserService() {
        // 初始化实体映射
        entityMap.put("user", WqUser.class);
        entityMap.put("wquser", WqUser.class);
        entityMap.put("community", Community.class);
        // 可以在这里添加更多实体映射
    }

    /**
     * 获取实体类
     */
    public Class<?> getEntityClass(String entityName) {
        Class<?> entityClass = entityMap.get(entityName.toLowerCase());
        if (entityClass == null) {
            throw new IllegalArgumentException("未知的实体类型: " + entityName);
        }
        return entityClass;
    }

    /**
     * 创建实体
     */
    @SuppressWarnings("unchecked")
    public Result<?> createByEntityName(String entityName, Map<String, Object> data) {
        try {
            Class<AbstractHibernateBean> entityClass = (Class<AbstractHibernateBean>) getEntityClass(entityName);
            AbstractHibernateBean entity = hibernateUtils.createEntityFromMap(entityClass, data);
            AbstractHibernateBean result = baseBeanSupport.create(entity);
            return Result.success("创建成功", result);
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 查询实体
     */
    public Result<?> queryByEntityName(String entityName, Map<String, Object> conditions,
                                      Integer pageNum, Integer pageSize, Map<String, Object> sort,
                                      List<String> fetch) {
        try {
            Class<?> entityClass = getEntityClass(entityName);

            // 数据类型转换
            Map<String, Object> convertedConditions = convertDataMap(entityClass, conditions);

            // 构建HQL查询
            HqlQuery hqlQuery = buildHql(entityClass, convertedConditions, sort, fetch);

            // 执行查询
            if (pageNum != null && pageSize != null) {
                // 分页查询
                Long total = baseBeanSupport.getCount(hqlQuery.getCountHql(), hqlQuery.getParams());
                List<?> results = baseBeanSupport.executeHqlPage(hqlQuery.getHql(), pageNum, pageSize, hqlQuery.getParams());

                Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
                Page<?> page = new PageImpl<>(results, pageable, total);

                // 使用简化的分页响应格式
                PageResult<?> pageResult = PageResult.of(page);
                return Result.success(pageResult);
            } else {
                // 普通查询
                List<?> results = baseBeanSupport.executeHql(hqlQuery.getHql(), hqlQuery.getParams());
                return Result.success(results);
            }
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 更新实体
     */
    @SuppressWarnings("unchecked")
    public Result<?> updateByEntityName(String entityName, String id, Map<String, Object> data) {
        try {
            Class<AbstractHibernateBean> entityClass = (Class<AbstractHibernateBean>) getEntityClass(entityName);
            AbstractHibernateBean entity = baseBeanSupport.findById(entityClass, id);

            if (entity == null) {
                return Result.error("实体不存在: " + id);
            }

            // 更新属性
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                try {
                    Field field = entityClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object convertedValue = hibernateUtils.convertValue(field.getType(), value);
                    field.set(entity, convertedValue);
                } catch (NoSuchFieldException e) {
                    // 忽略不存在的字段
                    continue;
                }
            }

            AbstractHibernateBean result = baseBeanSupport.update(entity);
            return Result.success("更新成功", result);
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除实体
     */
    @SuppressWarnings("unchecked")
    public Result<?> deleteByEntityName(String entityName, String id) {
        try {
            Class<AbstractHibernateBean> entityClass = (Class<AbstractHibernateBean>) getEntityClass(entityName);
            baseBeanSupport.deleteById(entityClass, id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 数据类型转换
     */
    private Map<String, Object> convertDataMap(Class<?> entityClass, Map<String, Object> conditions) {
        if (conditions == null) {
            return new HashMap<>();
        }

        Map<String, Object> converted = new HashMap<>();
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            converted.put(key, value); // 这里可以添加更复杂的类型转换逻辑
        }
        return converted;
    }

    /**
     * 构建HQL查询
     */
    private HqlQuery buildHql(Class<?> entityClass, Map<String, Object> conditions,
                             Map<String, Object> sort, List<String> fetch) {
        StringBuilder hql = new StringBuilder("from ").append(entityClass.getSimpleName()).append(" t");
        StringBuilder whereClause = new StringBuilder();
        List<Object> paramList = new ArrayList<>();

        // 关联查询支持
        if (fetch != null && !fetch.isEmpty()) {
            for (String fetchField : fetch) {
                hql.append(" left join fetch t.").append(fetchField);
            }
        }

        // 条件构建
        if (conditions != null && !conditions.isEmpty()) {
            whereClause.append(" where ");
            int[] paramIndex = {1};  // 使用数组来保存可变的索引
            buildWhereClause(whereClause, paramList, conditions, "t", paramIndex, entityClass);
        }

        hql.append(whereClause);

        // 添加排序
        if (sort != null && !sort.isEmpty()) {
            hql.append(" order by ");
            String orderClause = sort.entrySet().stream()
                .map(entry -> "t." + entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));
            hql.append(orderClause);
        }

        return new HqlQuery(hql.toString(), whereClause.toString(), paramList.toArray());
    }

    /**
     * 构建WHERE子句
     */
    private void buildWhereClause(StringBuilder whereClause, List<Object> paramList,
                                 Map<String, Object> conditions, String alias, int[] paramIndex, Class<?> entityClass) {
        List<String> clauses = new ArrayList<>();

        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("$and".equals(key) && value instanceof List) {
                List<Map<String, Object>> andConditions = (List<Map<String, Object>>) value;
                List<String> andClauses = new ArrayList<>();
                for (Map<String, Object> andCondition : andConditions) {
                    StringBuilder andClause = new StringBuilder();
                    buildWhereClause(andClause, paramList, andCondition, alias, paramIndex, entityClass);
                    if (andClause.length() > 0) {
                        andClauses.add("(" + andClause.toString() + ")");
                    }
                }
                if (!andClauses.isEmpty()) {
                    clauses.add("(" + String.join(" and ", andClauses) + ")");
                }
            } else if ("$or".equals(key) && value instanceof List) {
                List<Map<String, Object>> orConditions = (List<Map<String, Object>>) value;
                List<String> orClauses = new ArrayList<>();
                for (Map<String, Object> orCondition : orConditions) {
                    StringBuilder orClause = new StringBuilder();
                    buildWhereClause(orClause, paramList, orCondition, alias, paramIndex, entityClass);
                    if (orClause.length() > 0) {
                        orClauses.add("(" + orClause.toString() + ")");
                    }
                }
                if (!orClauses.isEmpty()) {
                    clauses.add("(" + String.join(" or ", orClauses) + ")");
                }
            } else {
                // 普通条件
                String operator = "=";
                Object paramValue = value;

                if (value instanceof Map) {
                    Map<String, Object> opMap = (Map<String, Object>) value;
                    if (opMap.containsKey("$eq")) {
                        operator = "=";
                        paramValue = opMap.get("$eq");
                    } else if (opMap.containsKey("$gt")) {
                        operator = ">";
                        paramValue = opMap.get("$gt");
                    } else if (opMap.containsKey("$gte")) {
                        operator = ">=";
                        paramValue = opMap.get("$gte");
                    } else if (opMap.containsKey("$lt")) {
                        operator = "<";
                        paramValue = opMap.get("$lt");
                    } else if (opMap.containsKey("$lte")) {
                        operator = "<=";
                        paramValue = opMap.get("$lte");
                    } else if (opMap.containsKey("$like")) {
                        // 检查字段类型，只对字符串类型使用like
                        try {
                            Field field = getDeclaredField(entityClass, key);
                            // 只对字符串类型使用like
                            if (field.getType() == String.class) {
                                operator = "like";
                                paramValue = "%" + opMap.get("$like") + "%";
                            } else {
                                // 非字符串类型转为等值查询
                                operator = "=";
                                paramValue = opMap.get("$like");
                            }
                        } catch (Exception e) {
                            // 如果获取字段失败，默认使用like
                            operator = "like";
                            paramValue = "%" + opMap.get("$like") + "%";
                        }
                    } else if (opMap.containsKey("$in")) {
                        operator = "in";
                        paramValue = opMap.get("$in");
                    } else if (opMap.containsKey("$notIn")) {
                        operator = "not in";
                        paramValue = opMap.get("$notIn");
                    }
                }

                // 处理 in 和 not in 运算符
                if ("in".equalsIgnoreCase(operator) || "not in".equalsIgnoreCase(operator)) {
                    if (paramValue instanceof List) {
                        List<?> values = (List<?>) paramValue;
                        StringBuilder inClause = new StringBuilder();
                        inClause.append(alias).append(".").append(key).append(" ").append(operator).append(" (");
                        for (int i = 0; i < values.size(); i++) {
                            if (i > 0) inClause.append(", ");
                            inClause.append("?").append(paramIndex[0]++);
                            // 转换值类型
                            Object convertedValue = convertConditionValue(values.get(i), entityClass, key);
                            paramList.add(convertedValue);
                        }
                        inClause.append(")");
                        clauses.add(inClause.toString());
                    }
                } else {
                    // 类型转换 - 需要知道字段类型
                    Object convertedValue = convertConditionValue(paramValue, entityClass, key);
                    clauses.add(alias + "." + key + " " + operator + " ?" + paramIndex[0]);
                    paramList.add(convertedValue);
                    paramIndex[0]++;
                }
            }
        }

        if (!clauses.isEmpty()) {
            whereClause.append(String.join(" and ", clauses));
        }
    }

    /**
     * 获取字段（包括父类）
     */
    private Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getDeclaredField(superClass, fieldName);
            }
            throw e;
        }
    }

    /**
     * 转换条件值（支持日期字符串转换和类型自动转换）
     */
    private Object convertConditionValue(Object value, Class<?> entityClass, String fieldName) {
        if (value == null) {
            return null;
        }

        // 如果已经是正确的类型，直接返回
        if (!(value instanceof String)) {
            return value;
        }

        String strValue = (String) value;

        // 尝试获取字段类型
        try {
            Field field = getDeclaredField(entityClass, fieldName);
            Class<?> fieldType = field.getType();

            // 尝试解析为日期时间
            if (fieldType == java.time.LocalDateTime.class) {
                if (strValue.matches("\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}.*")) {
                    try {
                        // 尝试解析为LocalDateTime
                        if (strValue.contains("T")) {
                            strValue = strValue.replace("T", " ");
                        }
                        return java.time.LocalDateTime.parse(strValue.replace(" ", "T"),
                            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        // 解析失败，继续尝试其他方式
                    }
                }
            }
            // Integer类型转换
            else if (fieldType == Integer.class || fieldType == int.class) {
                return Integer.parseInt(strValue);
            }
            // Long类型转换
            else if (fieldType == Long.class || fieldType == long.class) {
                return Long.parseLong(strValue);
            }
            // Double类型转换
            else if (fieldType == Double.class || fieldType == double.class) {
                return Double.parseDouble(strValue);
            }
            // Boolean类型转换
            else if (fieldType == Boolean.class || fieldType == boolean.class) {
                return Boolean.parseBoolean(strValue);
            }
        } catch (Exception e) {
            // 获取字段类型失败，返回原值
        }

        return value;
    }
}
