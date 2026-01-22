package com.example.wq.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HQL查询封装类
 */
@Data
@NoArgsConstructor
public class HqlQuery {

    private String hql;           // 完整的HQL语句
    private String whereClause;   // WHERE子句
    private Object[] params;      // 查询参数数组

    public HqlQuery(String hql, Object[] params) {
        this.hql = hql;
        this.params = params;
    }

    public HqlQuery(String selectHql, String whereClause, Object[] params) {
        this.hql = selectHql;
        this.whereClause = whereClause;
        this.params = params;
    }

    /**
     * 获取COUNT查询的HQL
     */
    public String getCountHql() {
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            return "select count(*) from " + getEntityNameFromHql() + " t " + whereClause;
        } else {
            return "select count(*) from " + getEntityNameFromHql() + " t";
        }
    }

    /**
     * 从HQL中提取实体名称
     */
    private String getEntityNameFromHql() {
        if (hql != null && hql.startsWith("from ")) {
            String[] parts = hql.substring(5).trim().split("\\s+");
            return parts[0];
        }
        return "";
    }
}
