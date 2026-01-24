package com.example.wq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxMiniappProperties {

    /**
     * 小程序 AppID
     */
    private String appId;

    /**
     * 小程序 AppSecret
     */
    private String appSecret;

    /**
     * code2session 接口地址
     */
    private String code2sessionUrl = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * 获取完整的 code2session URL
     */
    public String getCode2sessionUrl(String code) {
        return code2sessionUrl + "?appid=" + appId + "&secret=" + appSecret + "&js_code=" + code + "&grant_type=authorization_code";
    }
}
