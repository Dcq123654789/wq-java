package com.example.wq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * 事务管理配置
 *
 * Spring Boot 默认已自动配置事务管理，此配置类主要用于明确说明和自定义
 */
@Configuration
@EnableTransactionManagement  // 启用注解事务管理（其实Spring Boot默认已启用）
public class TransactionConfig {

    /**
     * 事务管理器
     * Spring Boot 会自动创建 DataSourceTransactionManager Bean
     * 这里只是为了说明，实际可以省略
     */
    // @Bean
    // public TransactionManager transactionManager(DataSource dataSource) {
    //     return new DataSourceTransactionManager(dataSource);
    // }
}
