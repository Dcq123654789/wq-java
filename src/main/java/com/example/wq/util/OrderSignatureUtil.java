package com.example.wq.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.TreeMap;

/**
 * 订单签名工具
 * 用于防止订单金额被篡改
 */
@Component
public class OrderSignatureUtil {

    private static final String SIGN_SALT = "wq_order_salt_2024"; // 签名盐值，生产环境应配置在环境变量
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成订单签名
     *
     * @param orderId    订单ID
     * @param totalAmount 订单总金额
     * @param timestamp  时间戳
     * @return 签名字符串
     */
    public String generateSignature(String orderId, java.math.BigDecimal totalAmount, long timestamp) {
        // 按字典序排序参数，防止参数顺序影响签名
        TreeMap<String, String> params = new TreeMap<>();
        params.put("orderId", orderId);
        params.put("amount", totalAmount.setScale(2, java.math.RoundingMode.HALF_UP).toString());
        params.put("timestamp", String.valueOf(timestamp));
        params.put("salt", SIGN_SALT);

        // 拼接参数
        StringBuilder sb = new StringBuilder();
        for (String value : params.values()) {
            sb.append(value);
        }

        // MD5 加密
        return md5Hex(sb.toString());
    }

    /**
     * 验证订单签名
     *
     * @param orderId    订单ID
     * @param totalAmount 订单总金额
     * @param timestamp  时间戳
     * @param signature  待验证的签名
     * @return true-签名有效，false-签名无效
     */
    public boolean verifySignature(String orderId, java.math.BigDecimal totalAmount, long timestamp, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }

        // 检查时间戳有效性（防止重放攻击，5分钟内有效）
        long currentTimestamp = System.currentTimeMillis();
        if (Math.abs(currentTimestamp - timestamp) > 5 * 60 * 1000) {
            return false;
        }

        String expectedSignature = generateSignature(orderId, totalAmount, timestamp);
        return expectedSignature.equals(signature);
    }

    /**
     * 生成随机 nonce
     *
     * @return nonce 字符串
     */
    public static String generateNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * MD5 加密
     */
    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 加密失败", e);
        }
    }
}
