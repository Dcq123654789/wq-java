package com.example.wq.service;

import com.example.wq.config.WeChatPayConfig;
import com.example.wq.entity.*;
import com.example.wq.enums.OrderStatus;
import com.example.wq.repository.InventoryLockRepository;
import com.example.wq.repository.OrderRepository;
import com.example.wq.repository.ProductRepository;
import com.example.wq.util.OrderSignatureUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 商城订单服务
 * 整合订单创建、库存预占、微信支付等功能
 */
@Service
public class MallOrderService {

    private static final Logger log = LoggerFactory.getLogger(MallOrderService.class);

    // 订单超时时间（分钟）
    private static final int ORDER_TIMEOUT_MINUTES = 15;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryLockRepository inventoryLockRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WeChatPayConfig weChatPayConfig;

    @Autowired
    private OrderSignatureUtil signatureUtil;

    @Autowired
    private RateLimiterService rateLimiterService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 提交订单（核心接口）
     * 包含：库存预占 + 订单创建 + 微信支付参数生成
     */
    @Transactional(rollbackFor = Exception.class)
    public SubmitOrderResult submitOrder(SubmitOrderRequest request) {
        log.info("开始提交订单: userId={}, items={}", request.getUserId(), request.getItems().size());

        SubmitOrderResult result = new SubmitOrderResult();

        try {
            // ========== 第一步：参数校验 ==========
            validateRequest(request);

            // ========== 第二步：用户限流检查 ==========
            String rateLimitKey = "order:create:" + request.getUserId();
            if (!rateLimiterService.allowRequest(rateLimitKey,
                    RateLimiterService.Limits.ORDER_CREATE_LIMIT,
                    RateLimiterService.Limits.ORDER_CREATE_WINDOW)) {
                throw new RuntimeException("操作过于频繁，请稍后再试");
            }

            // ========== 第三步：生成订单编号 ==========
            String orderNo = generateOrderNo();
            String orderId = null; // 稍后生成

            // ========== 第四步：处理订单明细 + 库存预占 + 金额计算 ==========
            List<OrderItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            long expireTime = System.currentTimeMillis() / 1000 + TimeUnit.MINUTES.toSeconds(ORDER_TIMEOUT_MINUTES);

            for (SubmitOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
                // 1. 获取商品信息
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new RuntimeException("商品不存在: " + itemRequest.getProductId()));

                // 2. 检查商品状态
                if (product.getStock() == null || product.getStock() <= 0) {
                    throw new RuntimeException("商品已下架或库存不足: " + product.getName());
                }

                // 3. 检查库存（包含预占的库存）
                int availableStock = getAvailableStock(product.get_id());
                if (availableStock < itemRequest.getQuantity()) {
                    throw new RuntimeException("商品库存不足: " + product.getName() + "（可用: " + availableStock + "）");
                }

                // 4. 验证数量
                if (itemRequest.getQuantity() <= 0 || itemRequest.getQuantity() > 999) {
                    throw new RuntimeException("购买数量不合法: " + itemRequest.getQuantity());
                }

                // 5. 后端计算金额（防止前端篡改价格）
                BigDecimal itemSubtotal = product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity()));

                // 6. 创建订单明细
                OrderItem item = new OrderItem();
                item.setProductId(product.get_id());
                item.setProductName(product.getName());
                item.setProductPrice(product.getPrice());
                item.setQuantity(itemRequest.getQuantity());
                item.setSubtotal(itemSubtotal);
                items.add(item);

                // 7. 预占库存
                InventoryLock lock = new InventoryLock();
                lock.setProductId(product.get_id());
                lock.setQuantity(itemRequest.getQuantity());
                lock.setExpireTime(expireTime);
                lock.setStatus(1); // 已锁定
                inventoryLockRepository.save(lock);

                totalAmount = totalAmount.add(itemSubtotal);
            }

            // ========== 第五步：创建订单 ==========
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId());
            order.setReceiverName(request.getReceiverName());
            order.setReceiverPhone(request.getReceiverPhone());
            order.setReceiverAddress(request.getReceiverAddress());
            order.setRemark(request.getRemark());
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.PENDING.getCode());
            order.setItems(items);
            orderRepository.save(order);
            orderId = order.get_id();

            // ========== 第六步：关联库存预占记录 ==========
            for (OrderItem item : items) {
                entityManager.createQuery(
                        "UPDATE InventoryLock SET orderId = :orderId WHERE productId = :productId AND status = 1 AND orderId IS NULL"
                )
                        .setParameter("orderId", orderId)
                        .setParameter("productId", item.getProductId())
                        .setMaxResults(item.getQuantity()) // 只更新本次预占的数量
                        .executeUpdate();
            }

            // ========== 第七步：生成订单签名（防止金额篡改）==========
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature(orderId, totalAmount, timestamp);

            // ========== 第八步：生成微信支付参数 ==========
            Map<String, Object> payParams = generateWeChatPayParams(order, totalAmount);

            // ========== 第九步：组装返回结果 ==========
            result.setSuccess(true);
            result.setMessage("订单创建成功");
            result.setOrderId(orderId);
            result.setOrderNo(orderNo);
            result.setTotalAmount(totalAmount);
            result.setExpireTime(expireTime);
            result.setSignature(signature);
            result.setTimestamp(timestamp);
            result.setPayParams(payParams);

            log.info("订单提交成功: orderNo={}, totalAmount={}", orderNo, totalAmount);

        } catch (Exception e) {
            log.error("提交订单失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 支付订单（支付回调后调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payOrder(String orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }

            if (order.getStatus() != OrderStatus.PENDING.getCode()) {
                throw new RuntimeException("订单状态不正确");
            }

            // 检查是否过期
            LocalDateTime createTime = order.getCreateTime();
            LocalDateTime expireTime = createTime.plusMinutes(ORDER_TIMEOUT_MINUTES);
            if (LocalDateTime.now().isAfter(expireTime)) {
                cancelOrder(orderId, "订单超时");
                throw new RuntimeException("订单已超时");
            }

            // 扣减库存
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);
                if (product != null) {
                    int newStock = product.getStock() - item.getQuantity();
                    if (newStock < 0) {
                        throw new RuntimeException("商品库存不足: " + product.getName());
                    }
                    product.setStock(newStock);
                    product.setSales(product.getSales() + item.getQuantity());
                    productRepository.save(product);
                }
            }

            // 释放库存预占
            inventoryLockRepository.releaseByOrderId(orderId);

            // 更新订单状态和支付时间
            order.setStatus(OrderStatus.PAID.getCode());
            order.setPayTime(LocalDateTime.now());
            orderRepository.save(order);

            result.put("success", true);
            result.put("message", "支付成功");

            log.info("订单支付成功: orderId={}", orderId);

        } catch (Exception e) {
            log.error("支付订单失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 取消订单（释放库存预占）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancelOrder(String orderId, String reason) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }

            if (order.getStatus() != OrderStatus.PENDING.getCode()) {
                throw new RuntimeException("只能取消待支付订单");
            }

            // 释放库存预占
            int released = inventoryLockRepository.releaseByOrderId(orderId);

            // 更新订单状态
            order.setStatus(OrderStatus.CANCELLED.getCode());
            orderRepository.save(order);

            result.put("success", true);
            result.put("message", "订单已取消");
            result.put("releasedStock", released);

            log.info("订单取消成功: orderId={}, reason={}, releasedStock={}", orderId, reason, released);

        } catch (Exception e) {
            log.error("取消订单失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 处理超时订单（定时任务调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleExpiredOrders() {
        Map<String, Object> result = new HashMap<>();

        try {
            LocalDateTime expireTime = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);
            List<Order> expiredOrders = orderRepository.findExpiredPendingOrders(expireTime);

            int cancelledCount = 0;
            int totalReleasedStock = 0;

            for (Order order : expiredOrders) {
                try {
                    Map<String, Object> cancelResult = cancelOrder(order.get_id(), "订单超时自动取消");
                    if (Boolean.TRUE.equals(cancelResult.get("success"))) {
                        cancelledCount++;
                        totalReleasedStock += (Integer) cancelResult.getOrDefault("releasedStock", 0);
                    }
                } catch (Exception e) {
                    log.error("取消超时订单失败: orderId={}, error={}", order.get_id(), e.getMessage());
                }
            }

            result.put("success", true);
            result.put("cancelledCount", cancelledCount);
            result.put("totalReleasedStock", totalReleasedStock);
            result.put("message", String.format("处理完成，取消订单 %d 个，释放库存 %d", cancelledCount, totalReleasedStock));

            log.info("处理超时订单完成: cancelledCount={}, totalReleasedStock={}", cancelledCount, totalReleasedStock);

        } catch (Exception e) {
            log.error("处理超时订单失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    // ========== 私有方法 ==========

    /**
     * 参数校验
     */
    private void validateRequest(SubmitOrderRequest request) {
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("订单商品不能为空");
        }
        if (request.getReceiverName() == null || request.getReceiverName().trim().isEmpty()) {
            throw new RuntimeException("收货人姓名不能为空");
        }
        if (request.getReceiverPhone() == null || !request.getReceiverPhone().matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("收货人电话格式不正确");
        }
        if (request.getReceiverAddress() == null || request.getReceiverAddress().trim().isEmpty()) {
            throw new RuntimeException("收货地址不能为空");
        }
    }

    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis();
    }

    /**
     * 获取可用库存（总库存 - 已预占库存）
     */
    private int getAvailableStock(String productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return 0;
        }

        Integer lockedQuantity = inventoryLockRepository.sumLockedQuantity(
                productId,
                System.currentTimeMillis() / 1000
        );

        int totalStock = product.getStock();
        int locked = lockedQuantity != null ? lockedQuantity : 0;

        return totalStock - locked;
    }

    /**
     * 生成微信支付参数
     * TODO: 集成真实的微信支付 SDK
     */
    private Map<String, Object> generateWeChatPayParams(Order order, BigDecimal totalAmount) {
        Map<String, Object> payParams = new HashMap<>();

        // 实际项目中需要集成微信支付 SDK
        // 以下是示例代码结构：
        /*
        WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
        request.setOutTradeNo(order.getOrderNo());
        request.setDescription("晚晴商城-商品购买");
        request.setAmount(new WxPayUnifiedOrderV3Request.Amount()
                .setTotal(totalAmount.multiply(new BigDecimal("100")).intValue())); // 单位：分
        request.setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(order.getUserId()));

        WxPayUnifiedOrderV3Response response = wxPayService.createOrder(request);

        payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        payParams.put("nonceStr", generateNonceStr());
        payParams.put("package", "prepay_id=" + response.getPrepayId());
        payParams.put("signType", "RSA");
        payParams.put("paySign", generateSign(payParams));
        */

        // 临时返回模拟数据
        payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        payParams.put("nonceStr", OrderSignatureUtil.generateNonce());
        payParams.put("package", "prepay_id=mock_prepay_id_" + order.getOrderNo());
        payParams.put("signType", "RSA");
        payParams.put("paySign", "mock_sign_" + System.currentTimeMillis());
        payParams.put("outTradeNo", order.getOrderNo());
        payParams.put("totalAmount", totalAmount);

        log.warn("当前返回的是模拟支付参数，需要集成微信支付 SDK");
        return payParams;
    }

    /**
     * 获取订单详情
     */
    public Map<String, Object> getOrderDetail(String orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new RuntimeException("订单不存在");
            }

            // 转换为 Map
            Map<String, Object> data = convertToOrderDetailMap(order);

            result.put("success", true);
            result.put("data", data);

            log.info("查询订单详情成功: orderId={}", orderId);

        } catch (Exception e) {
            log.error("查询订单详情失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户订单列表
     */
    public Map<String, Object> getUserOrders(String userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Order> orders = orderRepository.findByUserIdOrderByCreateTimeDesc(userId);

            List<Map<String, Object>> dataList = orders.stream()
                    .map(this::convertToOrderDetailMap)
                    .toList();

            result.put("success", true);
            result.put("data", dataList);

            log.info("查询用户订单列表成功: userId={}, count={}", userId, dataList.size());

        } catch (Exception e) {
            log.error("查询用户订单列表失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 将 Order 实体转换为 Map
     */
    private Map<String, Object> convertToOrderDetailMap(Order order) {
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.get_id());
        response.put("orderNo", order.getOrderNo());
        response.put("userId", order.getUserId());
        response.put("status", order.getStatus());

        OrderStatus orderStatus = OrderStatus.fromCode(order.getStatus());
        response.put("statusDesc", orderStatus != null ? orderStatus.getDescription() : "未知");

        response.put("totalAmount", order.getTotalAmount());
        response.put("receiverName", order.getReceiverName());
        response.put("receiverPhone", order.getReceiverPhone());
        response.put("receiverAddress", order.getReceiverAddress());
        response.put("remark", order.getRemark());
        response.put("createTime", order.getCreateTime());
        response.put("payTime", order.getPayTime());

        // 转换订单明细
        List<Map<String, Object>> itemResponses = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> itemResponse = new HashMap<>();
                    itemResponse.put("productId", item.getProductId());
                    itemResponse.put("productName", item.getProductName());
                    itemResponse.put("productPrice", item.getProductPrice());
                    itemResponse.put("quantity", item.getQuantity());
                    itemResponse.put("subtotal", item.getSubtotal());
                    return itemResponse;
                })
                .toList();

        response.put("items", itemResponses);

        return response;
    }

    // ========== 内部类 ==========

    /**
     * 提交订单请求
     */
    public static class SubmitOrderRequest {
        private String userId;
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String remark;
        private List<OrderItemRequest> items;

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
        public String getReceiverPhone() { return receiverPhone; }
        public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
        public String getReceiverAddress() { return receiverAddress; }
        public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public List<OrderItemRequest> getItems() { return items; }
        public void setItems(List<OrderItemRequest> items) { this.items = items; }

        public static class OrderItemRequest {
            private String productId;
            private Integer quantity;

            public String getProductId() { return productId; }
            public void setProductId(String productId) { this.productId = productId; }
            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }
        }
    }

    /**
     * 提交订单结果
     */
    public static class SubmitOrderResult {
        private boolean success;
        private String message;
        private String orderId;
        private String orderNo;
        private BigDecimal totalAmount;
        private Long expireTime;
        private String signature;
        private Long timestamp;
        private Map<String, Object> payParams;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public Long getExpireTime() { return expireTime; }
        public void setExpireTime(Long expireTime) { this.expireTime = expireTime; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getPayParams() { return payParams; }
        public void setPayParams(Map<String, Object> payParams) { this.payParams = payParams; }
    }
}
