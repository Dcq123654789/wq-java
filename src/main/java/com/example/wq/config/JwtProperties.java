package com.example.wq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 密钥（生产环境请使用更安全的密钥，建议从环境变量读取）
     */
    private String secret;

    /**
     * 访问令牌过期时间（毫秒），默认7天
     */
    private Long expiration = 604800000L;

    /**
     * 刷新令牌过期时间（毫秒），默认30天
     */
    private Long refreshExpiration = 2592000000L;

    /**
     * HTTP 头名称
     */
    private String header = "Authorization";

    /**
     * 令牌前缀
     */
    private String tokenPrefix = "Bearer";

    /**
     * 签发者
     */
    private String issuer = "wanqing";
}
