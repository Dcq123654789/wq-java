package com.example.wq.service;

import com.example.wq.config.JwtProperties;
import com.example.wq.entity.AdminUser;
import com.example.wq.enums.AdminUserStatus;
import com.example.wq.enums.DeletedFlag;
import com.example.wq.repository.AdminUserRepository;
import com.example.wq.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员认证服务
 */
@Slf4j
@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthService(AdminUserRepository adminUserRepository,
                           JwtTokenUtil jwtTokenUtil,
                           JwtProperties jwtProperties,
                           PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 管理员登录
     *
     * @param username 用户名
     * @param password 密码
     * @param request  HTTP请求（用于获取IP）
     * @return 登录响应
     */
    @Transactional
    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        // 1. 查找管理员
        AdminUser adminUser = adminUserRepository
                .findByUsernameAndDeleted(username, DeletedFlag.NOT_DELETED.getCode())
                .orElseThrow(() -> {
                    log.warn("管理员登录失败: 用户不存在 - {}", username);
                    return new RuntimeException("用户名或密码错误");
                });

        // 2. 验证密码（支持BCrypt加密和明文两种方式）
        boolean passwordValid = false;
        String storedPassword = adminUser.getPassword();

        if (storedPassword != null) {
            // 判断密码是否为BCrypt加密格式（以$2a$或$2b$开头）
            if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")) {
                // BCrypt加密密码，使用passwordEncoder验证
                passwordValid = passwordEncoder.matches(password, storedPassword);
            } else {
                // 明文密码，直接比较
                passwordValid = password.equals(storedPassword);
            }
        }

        if (!passwordValid) {
            // 增加登录失败次数
            adminUser.incrementLoginFailCount();
            adminUserRepository.save(adminUser);

            log.warn("管理员登录失败: 密码错误 - {}, 失败次数: {}",
                    username, adminUser.getLoginFailCount());

            // 检查是否需要锁定账户
            if (adminUser.getLoginFailCount() >= 5) {
                adminUser.setStatus(AdminUserStatus.LOCKED.getCode());
                adminUserRepository.save(adminUser);
                log.warn("管理员账户已锁定: {}", username);
                throw new RuntimeException("账户已被锁定，请联系管理员");
            }

            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 检查账户状态
        if (adminUser.getStatus() != null && adminUser.getStatus().equals(AdminUserStatus.DISABLED.getCode())) {
            log.warn("管理员登录失败: 账户已禁用 - {}", username);
            throw new RuntimeException("账户已被禁用");
        }

        if (adminUser.getStatus() != null && adminUser.getStatus().equals(AdminUserStatus.LOCKED.getCode())) {
            log.warn("管理员登录失败: 账户已锁定 - {}", username);
            throw new RuntimeException("账户已被锁定，请联系管理员");
        }

        // 4. 获取客户端IP
        String clientIp = getClientIp(request);

        // 5. 更新最后登录信息
        adminUser.updateLastLoginInfo(clientIp);
        adminUserRepository.save(adminUser);

        // 6. 生成 Token
        String accessToken = jwtTokenUtil.generateAdminAccessToken(
                adminUser.get_id(),
                adminUser.getUsername()
        );

        String refreshToken = jwtTokenUtil.generateAdminRefreshToken(
                adminUser.get_id(),
                adminUser.getUsername()
        );

        // 7. 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", jwtProperties.getExpiration() / 1000);
        response.put("admin", convertAdminUserToMap(adminUser));

        return response;
    }

    /**
     * 刷新管理员Token
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     */
    public Map<String, String> refreshAccessToken(String refreshToken) {
        // 1. 验证刷新令牌
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        // 2. 验证Token类型
        String tokenType = jwtTokenUtil.getTokenTypeFromToken(refreshToken);
        if (!JwtTokenUtil.TOKEN_TYPE_ADMIN.equals(tokenType)) {
            throw new RuntimeException("Token类型错误");
        }

        // 3. 从Token中获取管理员信息
        String adminId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        String username = jwtTokenUtil.getUsernameFromToken(refreshToken);

        if (adminId == null || username == null) {
            throw new RuntimeException("无法从刷新令牌中获取管理员信息");
        }

        // 4. 检查管理员是否存在
        AdminUser adminUser = adminUserRepository.findById(adminId)
                .filter(user -> user.getDeleted().equals(DeletedFlag.NOT_DELETED.getCode()))
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        // 5. 检查账户状态
        if (adminUser.getStatus() != null &&
            (adminUser.getStatus().equals(AdminUserStatus.DISABLED.getCode()) ||
             adminUser.getStatus().equals(AdminUserStatus.LOCKED.getCode()))) {
            throw new RuntimeException("账户已被禁用或锁定");
        }

        // 6. 生成新的访问令牌
        String newAccessToken = jwtTokenUtil.generateAdminAccessToken(adminId, username);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("tokenType", "Bearer");

        log.info("管理员Token刷新成功: adminId={}, username={}", adminId, username);

        return response;
    }

    /**
     * 根据Token获取管理员信息
     *
     * @param token JWT Token
     * @return 管理员信息
     */
    public AdminUser getAdminByToken(String token) {
        String adminId = jwtTokenUtil.getUserIdFromToken(token);
        if (adminId == null) {
            throw new RuntimeException("无法从Token中获取管理员ID");
        }

        return adminUserRepository.findById(adminId)
                .filter(user -> user.getDeleted().equals(DeletedFlag.NOT_DELETED.getCode()))
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
    }

    /**
     * 验证管理员Token
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        // 1. 验证Token格式和签名
        if (!jwtTokenUtil.validateToken(token)) {
            return false;
        }

        // 2. 验证Token类型
        String tokenType = jwtTokenUtil.getTokenTypeFromToken(token);
        if (!JwtTokenUtil.TOKEN_TYPE_ADMIN.equals(tokenType)) {
            log.warn("Token类型错误，期望admin，实际: {}", tokenType);
            return false;
        }

        // 3. 验证管理员是否存在且有效
        String adminId = jwtTokenUtil.getUserIdFromToken(token);
        if (adminId == null) {
            return false;
        }

        return adminUserRepository.findById(adminId)
                .filter(user -> user.getDeleted().equals(DeletedFlag.NOT_DELETED.getCode()))
                .map(user -> {
                    // 检查账户状态
                    if (user.getStatus() != null &&
                        (user.getStatus().equals(AdminUserStatus.DISABLED.getCode()) ||
                         user.getStatus().equals(AdminUserStatus.LOCKED.getCode()))) {
                        log.warn("管理员账户已被禁用或锁定: adminId={}", adminId);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * 将AdminUser转换为Map（不包含密码）
     */
    private Map<String, Object> convertAdminUserToMap(AdminUser adminUser) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", adminUser.get_id());
        map.put("username", adminUser.getUsername());
        map.put("realName", adminUser.getRealName());
        map.put("phone", adminUser.getPhone());
        map.put("email", adminUser.getEmail());
        map.put("avatar", adminUser.getAvatar());
        map.put("role", adminUser.getRole());
        map.put("roleDescription", adminUser.getRoleEnum() != null ?
                adminUser.getRoleEnum().getDescription() : "");
        map.put("lastLoginTime", adminUser.getLastLoginTime());
        map.put("lastLoginIp", adminUser.getLastLoginIp());
        return map;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（X-Forwarded-For可能包含多个IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
