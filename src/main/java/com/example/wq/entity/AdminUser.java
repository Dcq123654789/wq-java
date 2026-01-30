package com.example.wq.entity;

import com.example.wq.annotation.ExcludeField;
import com.example.wq.enums.AdminUserRole;
import com.example.wq.enums.AdminUserStatus;
import com.example.wq.enums.DeletedFlag;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 后台管理系统管理员实体
 */
@Entity
@Table(name = "admin_user")
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台管理系统管理员实体")
public class AdminUser extends AbstractHibernateBean {

    @Schema(description = "用户名（登录账号）", example = "admin")
    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @JsonIgnore
    @Schema(description = "密码（加密存储）", example = "********", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    @Column(name = "real_name", length = 50)
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    @Column(name = "phone", length = 20)
    private String phone;

    @Schema(description = "邮箱", example = "admin@example.com")
    @Column(name = "email", length = 100)
    private String email;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    @Column(name = "avatar", length = 255)
    private String avatar;

    @Schema(description = "角色", example = "1", allowableValues = {"0", "1", "2"})
    @Column(name = "role", nullable = false)
    private Integer role = AdminUserRole.NORMAL_ADMIN.getCode();

    @Schema(description = "最后登录时间", example = "2024-01-22 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP", example = "192.168.1.100")
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Schema(description = "登录失败次数", example = "0")
    @Column(name = "login_fail_count")
    private Integer loginFailCount = 0;

    @ExcludeField
    @Schema(description = "逻辑删除标记", example = "0", allowableValues = {"0", "1"})
    @Column(name = "deleted")
    private Integer deleted = DeletedFlag.NOT_DELETED.getCode();

    /**
     * 获取角色枚举
     */
    @Transient
    public AdminUserRole getRoleEnum() {
        return AdminUserRole.fromCode(this.role);
    }

    /**
     * 设置角色
     */
    public void setRoleEnum(AdminUserRole adminUserRole) {
        this.role = adminUserRole != null ? adminUserRole.getCode() : AdminUserRole.NORMAL_ADMIN.getCode();
    }

 
    /**
     * 设置状态
     */
 
    /**
     * 获取删除标记枚举
     */
    @Transient
    public DeletedFlag getDeletedEnum() {
        return DeletedFlag.fromCode(this.deleted);
    }

    /**
     * 设置删除标记
     */
    public void setDeletedEnum(DeletedFlag deletedFlag) {
        this.deleted = deletedFlag != null ? deletedFlag.getCode() : null;
    }

    /**
     * 增加登录失败次数
     */
    public void incrementLoginFailCount() {
        this.loginFailCount = (this.loginFailCount != null ? this.loginFailCount : 0) + 1;
    }

    /**
     * 重置登录失败次数
     */
    public void resetLoginFailCount() {
        this.loginFailCount = 0;
    }

    /**
     * 更新最后登录信息
     */
    public void updateLastLoginInfo(String ip) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = ip;
        this.resetLoginFailCount();
    }

    @PrePersist
    protected void onCreate() {
        if (get_id() == null || get_id().isEmpty()) {
            set_id(generateId());   
        }
      
        if (this.role == null) {
            this.role = AdminUserRole.NORMAL_ADMIN.getCode();
        }
        if (this.deleted == null) {
            this.deleted = DeletedFlag.NOT_DELETED.getCode();
        }
        if (this.loginFailCount == null) {
            this.loginFailCount = 0;
        }
    }
}
