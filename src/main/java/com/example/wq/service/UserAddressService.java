package com.example.wq.service;

import com.example.wq.entity.UserAddress;
import com.example.wq.enums.YesNo;
import com.example.wq.repository.UserAddressRepository;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户收货地址服务
 */
@Service
public class UserAddressService {

    private static final Logger log = LoggerFactory.getLogger(UserAddressService.class);

    // 最大地址数量限制
    private static final int MAX_ADDRESS_COUNT = 20;

    @Autowired
    private UserAddressRepository userAddressRepository;

    /**
     * 查询用户的所有地址
     */
    public Map<String, Object> getUserAddresses(String userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIsDefaultDescUsedCountDesc(userId);
            result.put("success", true);
            result.put("data", addresses);
            result.put("total", addresses.size());
        } catch (Exception e) {
            log.error("查询用户地址失败: userId={}, error={}", userId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 添加收货地址
     */
    @Transactional
    public Map<String, Object> addAddress(UserAddress address) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 检查地址数量限制
            long count = userAddressRepository.countByUserId(address.getUserId());
            if (count >= MAX_ADDRESS_COUNT) {
                throw new RuntimeException("地址数量已达上限（" + MAX_ADDRESS_COUNT + "个）");
            }

            // 2. 如果设置为默认地址，先取消其他默认地址
            if (address.getIsDefault() != null && address.getIsDefault().equals(YesNo.YES.getCode())) {
                userAddressRepository.cancelAllDefaultByUserId(address.getUserId(), YesNo.NO.getCode());
            }

            // 3. 保存地址
            UserAddress saved = userAddressRepository.save(address);

            result.put("success", true);
            result.put("data", saved);
            result.put("message", "添加成功");

            log.info("添加收货地址成功: userId={}, addressId={}", address.getUserId(), saved.get_id());

        } catch (Exception e) {
            log.error("添加收货地址失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 更新收货地址
     */
    @Transactional
    public Map<String, Object> updateAddress(String addressId, UserAddress address) {
        Map<String, Object> result = new HashMap<>();

        try {
            UserAddress existing = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("地址不存在"));

            // 1. 检查权限
            if (!existing.getUserId().equals(address.getUserId())) {
                throw new RuntimeException("无权操作该地址");
            }

            // 2. 如果设置为默认地址，先取消其他默认地址
            if (address.getIsDefault() != null && address.getIsDefault().equals(YesNo.YES.getCode())) {
                userAddressRepository.cancelAllDefaultByUserId(address.getUserId(), YesNo.NO.getCode());
            }

            // 3. 更新字段
            existing.setReceiverName(address.getReceiverName());
            existing.setReceiverPhone(address.getReceiverPhone());
            existing.setProvince(address.getProvince());
            existing.setCity(address.getCity());
            existing.setDistrict(address.getDistrict());
            existing.setDetailAddress(address.getDetailAddress());
            existing.setPostalCode(address.getPostalCode());
            if (address.getIsDefault() != null) {
                existing.setIsDefault(address.getIsDefault());
            }
            existing.setTag(address.getTag());

            UserAddress updated = userAddressRepository.save(existing);

            result.put("success", true);
            result.put("data", updated);
            result.put("message", "更新成功");

            log.info("更新收货地址成功: addressId={}", addressId);

        } catch (Exception e) {
            log.error("更新收货地址失败: addressId={}, error={}", addressId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 删除收货地址
     */
    @Transactional
    public Map<String, Object> deleteAddress(String userId, String addressId) {
        Map<String, Object> result = new HashMap<>();

        try {
            UserAddress address = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("地址不存在"));

            // 检查权限
            if (!address.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作该地址");
            }

            userAddressRepository.deleteById(addressId);

            result.put("success", true);
            result.put("message", "删除成功");

            log.info("删除收货地址成功: addressId={}", addressId);

        } catch (Exception e) {
            log.error("删除收货地址失败: addressId={}, error={}", addressId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 设置默认地址
     */
    @Transactional
    public Map<String, Object> setDefaultAddress(String userId, String addressId) {
        Map<String, Object> result = new HashMap<>();

        try {
            UserAddress address = userAddressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("地址不存在"));

            // 检查权限
            if (!address.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作该地址");
            }

            // 取消所有默认地址，设置新的默认地址
            userAddressRepository.cancelAllDefaultByUserId(userId, YesNo.NO.getCode());
            address.setIsDefault(YesNo.YES.getCode());
            userAddressRepository.save(address);

            result.put("success", true);
            result.put("message", "设置成功");

            log.info("设置默认地址成功: userId={}, addressId={}", userId, addressId);

        } catch (Exception e) {
            log.error("设置默认地址失败: userId={}, addressId={}, error={}", userId, addressId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户默认地址
     */
    public Map<String, Object> getDefaultAddress(String userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<UserAddress> address = userAddressRepository.findByUserIdAndIsDefault(userId, YesNo.YES.getCode());

            if (address.isPresent()) {
                result.put("success", true);
                result.put("data", address.get());
            } else {
                result.put("success", false);
                result.put("message", "未设置默认地址");
            }

        } catch (Exception e) {
            log.error("获取默认地址失败: userId={}, error={}", userId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 增加地址使用次数（下单成功后调用）
     */
    @Transactional
    public void incrementUsedCount(String addressId) {
        try {
            UserAddress address = userAddressRepository.findById(addressId).orElse(null);
            if (address != null) {
                int count = address.getUsedCount() != null ? address.getUsedCount() : 0;
                address.setUsedCount(count + 1);
                userAddressRepository.save(address);
                log.debug("增加地址使用次数: addressId={}, usedCount={}", addressId, count + 1);
            }
        } catch (Exception e) {
            log.error("增加地址使用次数失败: addressId={}, error={}", addressId, e.getMessage());
        }
    }
}
