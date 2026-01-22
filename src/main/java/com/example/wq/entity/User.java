package com.example.wq.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Entity
@Table(name = "user")
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户实体")
public class User extends AbstractHibernateBean {

    @Schema(description = "用户名", example = "zhangsan", required = true)
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Schema(description = "密码", example = "123456")
    @Column(name = "password")
    private String password;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Column(name = "email")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @Column(name = "phone")
    private String phone;

    @Schema(description = "用户状态", example = "1", allowableValues = {"1", "0"})
    @Column(name = "status")
    private Integer status = 1; // 1-正常, 0-禁用

    @Schema(description = "用户角色", example = "user")
    @Column(name = "role")
    private String role;
}

