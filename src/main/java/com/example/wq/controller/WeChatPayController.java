package com.example.wq.controller;

import com.example.wq.entity.Result;
import com.example.wq.service.WeChatPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat/pay")
@Tag(name = "微信支付", description = "微信支付相关接口")
public class WeChatPayController {

    private final WeChatPayService weChatPayService;

    public WeChatPayController(WeChatPayService weChatPayService) {
        this.weChatPayService = weChatPayService;
    }   

    /**
     * 获取微信支付参数
     */
    @PostMapping("/params")
    @Operation(summary = "获取支付参数", description = "根据报名记录获取微信支付参数")
    public Result<Map<String, Object>> getPayParams(@RequestBody Map<String, String> request) {

        String registrationId = request.get("registrationId");
        String orderNo = request.get("orderNo");

        // 参数校验
        if (registrationId == null || registrationId.trim().isEmpty()) {
            return Result.error("报名记录ID不能为空");
        }
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return Result.error("订单号不能为空");
        }

        try {
            Map<String, Object> payParams = weChatPayService.getPayParams(registrationId, orderNo);
            return Result.success("获取支付参数成功", payParams);
        } catch (RuntimeException e) {
            log.error("获取支付参数失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 微信支付回调接口
     */
    @PostMapping("/callback")
    @Operation(summary = "支付回调", description = "接收微信支付结果通知")
    public String handlePayCallback(@RequestBody String requestBody) {
        try {
            log.info("收到微信支付回调: {}", requestBody);
            weChatPayService.handlePayCallback(requestBody);
            return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
        } catch (Exception e) {
            log.error("处理支付回调失败: {}", e.getMessage(), e);
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[" + e.getMessage() + "]]></return_msg></xml>";
        }
    }

    /**
     * 查询支付状态
     */
    @PostMapping("/status")
    @Operation(summary = "查询支付状态", description = "查询订单支付状态（用于前端轮询）")
    public Result<Map<String, Object>> queryPayStatus(@RequestBody Map<String, String> request) {

        String registrationId = request.get("registrationId");
        String orderNo = request.get("orderNo");

        // 参数校验
        if (registrationId == null || registrationId.trim().isEmpty()) {
            return Result.error("报名记录ID不能为空");
        }
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return Result.error("订单号不能为空");
        }

        try {
            Map<String, Object> status = weChatPayService.queryPayStatus(registrationId, orderNo);
            return Result.success("查询成功", status);
        } catch (RuntimeException e) {
            log.error("查询支付状态失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消未支付订单
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消未支付订单", description = "取消未支付的订单，释放名额")
    public Result<String> cancelUnpaidOrder(@RequestBody Map<String, String> request) {

        String registrationId = request.get("registrationId");
        String orderNo = request.get("orderNo");

        // 参数校验
        if (registrationId == null || registrationId.trim().isEmpty()) {
            return Result.error("报名记录ID不能为空");
        }
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return Result.error("订单号不能为空");
        }

        try {
            weChatPayService.cancelUnpaidOrder(registrationId, orderNo);
            return Result.success("订单取消成功");
        } catch (RuntimeException e) {
            log.error("取消订单失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
