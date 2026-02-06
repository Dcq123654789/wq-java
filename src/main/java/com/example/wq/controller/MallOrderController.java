package com.example.wq.controller;

import com.example.wq.entity.Result;
import com.example.wq.service.MallOrderService;
import com.example.wq.util.OrderSignatureUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商城订单控制器
 * 提供商城订单相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/mall/order")
@Tag(name = "商城订单管理", description = "老年商城订单相关接口")
public class MallOrderController {

    @Autowired
    private MallOrderService mallOrderService;

    @Autowired
    private OrderSignatureUtil signatureUtil;

    /**
     * 提交订单（核心接口）
     *
     * 功能：
     * 1. 参数校验
     * 2. 用户限流检查（防刷）
     * 3. 库存预占（防超卖）
     * 4. 订单创建
     * 5. 金额安全校验（后端重新计算）
     * 6. 生成订单签名（防篡改）
     * 7. 返回微信支付参数
     */
    @PostMapping("/submit")
    @Operation(summary = "提交订单", description = "创建订单并预占库存，返回支付参数")
    public Result<SubmitOrderResponse> submitOrder(@Valid @RequestBody SubmitOrderRequest request) {
        log.info("收到提交订单请求: userId={}, items={}", request.getUserId(), request.getItems().size());

        try {
            // 转换请求对象
            MallOrderService.SubmitOrderRequest serviceRequest = new MallOrderService.SubmitOrderRequest();
            serviceRequest.setUserId(request.getUserId());
            serviceRequest.setReceiverName(request.getReceiverName());
            serviceRequest.setReceiverPhone(request.getReceiverPhone());
            serviceRequest.setReceiverAddress(request.getReceiverAddress());
            serviceRequest.setRemark(request.getRemark());

            List<MallOrderService.SubmitOrderRequest.OrderItemRequest> items = request.getItems().stream()
                    .map(item -> {
                        MallOrderService.SubmitOrderRequest.OrderItemRequest serviceItem =
                                new MallOrderService.SubmitOrderRequest.OrderItemRequest();
                        serviceItem.setProductId(item.getProductId());
                        serviceItem.setQuantity(item.getQuantity());
                        return serviceItem;
                    })
                    .toList();
            serviceRequest.setItems(items);

            // 调用服务层
            MallOrderService.SubmitOrderResult result = mallOrderService.submitOrder(serviceRequest);

            if (!result.isSuccess()) {
                return Result.error(result.getMessage());
            }

            // 组装返回结果
            SubmitOrderResponse response = new SubmitOrderResponse();
            response.setOrderId(result.getOrderId());
            response.setOrderNo(result.getOrderNo());
            response.setTotalAmount(result.getTotalAmount());
            response.setExpireTime(result.getExpireTime());
            response.setSignature(result.getSignature());
            response.setTimestamp(result.getTimestamp());
            response.setPayParams(result.getPayParams());

            log.info("订单提交成功: orderNo={}", result.getOrderNo());
            return Result.success("订单创建成功", response);

        } catch (Exception e) {
            log.error("提交订单失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay/{orderId}")
    @Operation(summary = "支付订单", description = "完成订单支付，扣减库存")
    public Result<String> payOrder(
            @Parameter(description = "订单ID") @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> request) {

        // 验证签名（防止金额篡改）
        if (request != null && request.containsKey("signature")) {
            String signature = request.get("signature");
            Long timestamp = Long.parseLong(request.get("timestamp"));

            // TODO: 验证签名
            // boolean valid = signatureUtil.verifySignature(orderId, ... , timestamp, signature);
            // if (!valid) {
            //     return Result.error("签名验证失败");
            // }
        }

        Map<String, Object> result = mallOrderService.payOrder(orderId);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success("支付成功");
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderId}")
    @Operation(summary = "取消订单", description = "取消订单并释放预占库存")
    public Result<String> cancelOrder(
            @Parameter(description = "订单ID") @PathVariable String orderId,
            @RequestParam(defaultValue = "用户取消") String reason) {

        Map<String, Object> result = mallOrderService.cancelOrder(orderId, reason);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success("订单已取消");
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 处理超时订单（定时任务调用）
     */
    @PostMapping("/handle-expired")
    @Operation(summary = "处理超时订单", description = "定时任务：自动取消超时未支付订单")
    public Result<String> handleExpiredOrders() {
        Map<String, Object> result = mallOrderService.handleExpiredOrders();
        return Result.success((String) result.get("message"));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{orderId}")
    @Operation(summary = "获取订单详情", description = "根据订单ID查询订单详情")
    public Result<Map<String, Object>> getOrderDetail(
            @Parameter(description = "订单ID") @PathVariable String orderId) {

        try {
            Map<String, Object> result = mallOrderService.getOrderDetail(orderId);

            if (Boolean.TRUE.equals(result.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                return Result.success("查询成功", data);
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (Exception e) {
            log.error("查询订单详情失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户订单列表", description = "查询用户的订单列表")
    public Result<java.util.List<Map<String, Object>>> getUserOrders(
            @Parameter(description = "用户ID") @RequestParam String userId) {

        try {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> orders = (java.util.List<Map<String, Object>>)
                    mallOrderService.getUserOrders(userId).get("data");

            return Result.success("查询成功", orders);
        } catch (Exception e) {
            log.error("查询订单列表失败: {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    // ========== 请求/响应 DTO ==========

    /**
     * 提交订单请求
     */
    @Data
    public static class SubmitOrderRequest {

        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @NotBlank(message = "收货人姓名不能为空")
        private String receiverName;

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @NotBlank(message = "收货人电话不能为空")
        private String receiverPhone;

        @NotBlank(message = "收货地址不能为空")
        private String receiverAddress;

        private String remark;

        @NotEmpty(message = "商品列表不能为空")
        private List<OrderItemRequest> items;

        @Data
        public static class OrderItemRequest {
            @NotBlank(message = "商品ID不能为空")
            private String productId;

            @Positive(message = "购买数量必须大于0")
            private Integer quantity;
        }
    }

    /**
     * 提交订单响应
     */
    @Data
    public static class SubmitOrderResponse {
        private String orderId;          // 订单ID
        private String orderNo;          // 订单编号
        private java.math.BigDecimal totalAmount;  // 订单总金额
        private Long expireTime;         // 过期时间（秒级时间戳）
        private String signature;        // 订单签名（用于防篡改）
        private Long timestamp;          // 时间戳
        private Map<String, Object> payParams; // 微信支付参数
    }
}
