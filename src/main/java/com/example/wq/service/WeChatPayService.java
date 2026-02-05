package com.example.wq.service;

import com.example.wq.entity.ActivityRegistration;
import com.example.wq.entity.CommunityActivity;
import com.example.wq.enums.PaymentStatus;
import com.example.wq.repository.ActivityRegistrationRepository;
import com.example.wq.repository.CommunityActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 微信支付服务
 */
@Slf4j
@Service
public class WeChatPayService {

    private final ActivityRegistrationRepository registrationRepository;
    private final CommunityActivityRepository activityRepository;

    public WeChatPayService(ActivityRegistrationRepository registrationRepository,
                            CommunityActivityRepository activityRepository) {
        this.registrationRepository = registrationRepository;
        this.activityRepository = activityRepository;
    }

    /**
     * 获取微信支付参数
     *
     * @param registrationId 报名记录ID
     * @param orderNo        订单号
     * @return 支付参数
     */
    public Map<String, Object> getPayParams(String registrationId, String orderNo) {
        log.info("获取支付参数: registrationId={}, orderNo={}", registrationId, orderNo);

        // 1. 查询报名记录
        ActivityRegistration registration = registrationRepository.findByIdAndOrderNo(registrationId, orderNo)
                .orElseThrow(() -> new RuntimeException("报名记录不存在"));

        // 2. 检查报名状态
        if (registration.getStatus() != 0) {
            throw new RuntimeException("报名状态异常");
        }

        // 3. 检查支付状态
        if (registration.getPaymentStatus() == PaymentStatus.PAID.getCode()) {
            throw new RuntimeException("订单已支付");
        }

        // 4. 检查支付是否过期
        if (registration.getPaymentExpireTime() != null &&
                registration.getPaymentExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("订单已过期，请重新报名");
        }

        // 5. 查询活动信息
        CommunityActivity activity = activityRepository.findById(registration.getActivityId())
                .orElseThrow(() -> new RuntimeException("活动不存在"));

        // 6. 检查是否为付费活动
        if (activity.getPrice() == null || activity.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("免费活动无需支付");
        }

        // 7. 调用微信支付统一下单接口
        Map<String, Object> payParams = createWeChatPayOrder(registration, activity);

        log.info("支付参数生成成功: orderNo={}", orderNo);
        return payParams;
    }

    /**
     * 调用微信支付统一下单接口
     * TODO: 需要集成微信支付 SDK
     *
     * @param registration 报名记录
     * @param activity     活动信息
     * @return 支付参数
     */
    private Map<String, Object> createWeChatPayOrder(ActivityRegistration registration, CommunityActivity activity) {
        // TODO: 实际项目中需要集成微信支付 SDK
        // 以下是示例代码结构

        /*
        WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
        request.setOutTradeNo(registration.getOrderNo());
        request.setDescription(activity.getTitle());
        request.setAmount(new WxPayUnifiedOrderV3Request.Amount()
                .setTotal(registration.getPaymentAmount().multiply(new BigDecimal("100")).intValue())); // 单位：分
        request.setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(registration.getUserId()));

        // 调用微信支付接口
        WxPayUnifiedOrderV3Response response = wxPayService.createOrder(request);

        // 构造返回给小程序的支付参数
        Map<String, Object> result = new HashMap<>();
        result.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        result.put("nonceStr", generateNonceStr());
        result.put("package", "prepay_id=" + response.getPrepayId());
        result.put("signType", "RSA");
        result.put("paySign", generateSign(result));

        return result;
        */

        // 临时返回模拟数据
        Map<String, Object> result = new HashMap<>();
        result.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        result.put("nonceStr", "mock_nonce_str_" + System.currentTimeMillis());
        result.put("package", "prepay_id=mock_prepay_id_" + registration.getOrderNo());
        result.put("signType", "RSA");
        result.put("paySign", "mock_sign_" + System.currentTimeMillis());
        result.put("outTradeNo", registration.getOrderNo());
        result.put("totalAmount", registration.getPaymentAmount());

        log.warn("当前返回的是模拟支付参数，需要集成微信支付 SDK");
        return result;
    }

    /**
     * 处理微信支付回调
     * TODO: 需要实现支付验签和解析
     *
     * @param requestBody 回调数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePayCallback(String requestBody) {
        log.info("处理支付回调: {}", requestBody);

        // TODO: 实际项目中需要：
        // 1. 验证签名
        // 2. 解密回调数据
        // 3. 验证订单金额

        // 示例代码结构：
        /*
        // 解析回调数据
        WxPayPayNotifyV3Result result = wxPayService.parseNotifyV3(requestBody);

        // 获取订单信息
        String outTradeNo = result.getResult().getOutTradeNo();
        String transactionId = result.getResult().getTransactionId();
        Integer totalAmount = result.getResult().getAmount().getTotal();

        // 查询订单
        ActivityRegistration registration = registrationRepository.findByOrderNo(outTradeNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 检查是否已支付
        if (registration.getPaymentStatus() == PaymentStatus.PAID.getCode()) {
            log.warn("订单已支付，忽略回调: orderNo={}", outTradeNo);
            return;
        }

        // 更新支付状态
        registration.setPaymentStatus(PaymentStatus.PAID.getCode());
        registration.setPaymentTime(LocalDateTime.now());
        registration.setTransactionId(transactionId);
        registrationRepository.save(registration);

        log.info("支付成功，订单状态已更新: orderNo={}", outTradeNo);
        */

        // 临时：模拟支付成功处理
        log.warn("当前是模拟支付回调，需要实现真实的回调处理逻辑");
    }

    /**
     * 查询支付状态（轮询使用）
     *
     * @param registrationId 报名记录ID
     * @param orderNo        订单号
     * @return 支付状态
     */
    public Map<String, Object> queryPayStatus(String registrationId, String orderNo) {
        log.info("查询支付状态: registrationId={}, orderNo={}", registrationId, orderNo);

        ActivityRegistration registration = registrationRepository.findByIdAndOrderNo(registrationId, orderNo)
                .orElseThrow(() -> new RuntimeException("报名记录不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("paymentStatus", registration.getPaymentStatus());
        PaymentStatus status = PaymentStatus.fromCode(registration.getPaymentStatus());
        result.put("paymentStatusDesc", status != null ? status.getDescription() : "");
        result.put("paymentTime", registration.getPaymentTime());
        result.put("isPaid", registration.getPaymentStatus() == PaymentStatus.PAID.getCode());

        // 检查是否过期
        boolean isExpired = registration.getPaymentExpireTime() != null &&
                registration.getPaymentExpireTime().isBefore(LocalDateTime.now());
        result.put("isExpired", isExpired);

        return result;
    }

    /**
     * 取消未支付的订单（释放名额）
     *
     * @param registrationId 报名记录ID
     * @param orderNo        订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelUnpaidOrder(String registrationId, String orderNo) {
        log.info("取消未支付订单: registrationId={}, orderNo={}", registrationId, orderNo);

        ActivityRegistration registration = registrationRepository.findByIdAndOrderNo(registrationId, orderNo)
                .orElseThrow(() -> new RuntimeException("报名记录不存在"));

        // 只有未支付的订单才能取消
        if (registration.getPaymentStatus() != PaymentStatus.UNPAID.getCode()) {
            throw new RuntimeException("订单状态不允许取消");
        }

        // 更新报名状态为已取消
        registration.setStatus(1); // 1=已取消
        registration.setCancelTime(LocalDateTime.now());
        registration.setCancelReason("支付超时，订单自动取消");
        registrationRepository.save(registration);

        // 减少活动参与人数
        activityRepository.findById(registration.getActivityId()).ifPresent(activity -> {
            if (activity.getCurrentParticipants() > 0) {
                activity.setCurrentParticipants(activity.getCurrentParticipants() - 1);
                activityRepository.save(activity);
            }
        });

        log.info("订单取消成功: orderNo={}", orderNo);
    }
}
