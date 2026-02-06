package com.example.wq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置
 * 从 application.yml 读取配置，或从环境变量读取
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.pay")
public class WeChatPayConfig {

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 商户API证书序列号
     */
    private String mchSerialNo;

    /**
     * 商户API私钥路径
     */
    private String privateKeyPath;

    /**
     * 商户API私钥内容（直接配置）
     */
    private String privateKey;

    /**
     * 微信支付平台证书路径
     */
    private String platformCertPath;

    /**
     * API v3 密钥
     */
    private String apiV3Key;

    /**
     * 微信AppID
     */
    private String appId;

    /**
     * 微信AppSecret
     */
    private String appSecret;

    /**
     * 通知URL
     */
    private String notifyUrl;

    /**
     * 是否启用沙箱环境（测试时使用）
     */
    private boolean sandbox = false;
}
