package com.example.wq.controller.乐享;

import com.example.wq.entity.ActivityRegistration;
import com.example.wq.entity.Result;
import com.example.wq.service.CommunityActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 社区活动控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "社区活动管理", description = "社区活动查询和报名相关接口")
public class CommunityActivityController {

    private final CommunityActivityService communityActivityService;

    public CommunityActivityController(CommunityActivityService communityActivityService) {
        this.communityActivityService = communityActivityService;
    }

 

    /**
     * 快速检查用户是否已报名（仅返回布尔值）
     */
    @PostMapping("/community-activity")
    @Operation(summary = "快速检查报名状态", description = "快速查询用户是否已报名某个活动，仅返回true/false")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "查询报名状态请求参数<br><b>参数说明：</b><br>" +
                "- <code>activityId</code>: 必填，活动ID<br>" +
                "- <code>userId</code>: 必填，用户ID",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = "{\"activityId\":\"1234567890\",\"userId\":\"user123\"}"
            )
        )
    )
    public Result<Boolean> isUserRegistered(@RequestBody Map<String, String> request) {

        String activityId = request.get("activityId");
        String userId = request.get("userId");

        if (activityId == null || activityId.trim().isEmpty()) {
            return Result.error("活动ID不能为空");
        }

        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("用户ID不能为空");
        }

        boolean registered = communityActivityService.isUserRegistered(activityId, userId);

        return Result.success("查询成功", registered);
    }

    /**
     * 用户报名参加活动（优化版：支持免费/付费活动，高并发安全）
     */
    @PostMapping("/community-activity/register")
    @Operation(summary = "报名参加活动", description = "用户报名参加社区活动。免费活动直接报名成功，付费活动需完成支付。" +
            "使用事务控制+行锁+原子扣减，保证高并发下的数据一致性。")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "报名请求参数<br><b>参数说明：</b><br>" +
                "- <code>activityId</code>: 必填，活动ID<br>" +
                "- <code>userId</code>: 必填，用户ID<br>" +
                "- <code>userName</code>: 必填，用户姓名<br>" +
                "- <code>userPhone</code>: 可选，联系电话<br>" +
                "- <code>remarks</code>: 可选，备注信息",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = "{\"activityId\":\"1234567890\",\"userId\":\"user123\",\"userName\":\"张阿姨\",\"userPhone\":\"139****1234\",\"remarks\":\"期待参加活动\"}"
            )
        )
    )
    public Result<Map<String, Object>> registerActivity(@RequestBody Map<String, String> request) {

        String activityId = request.get("activityId");
        String userId = request.get("userId");
        String userName = request.get("userName");
        String userPhone = request.get("userPhone");
        String remarks = request.get("remarks");

        // 参数校验
        if (activityId == null || activityId.trim().isEmpty()) {
            return Result.error("活动ID不能为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        if (userName == null || userName.trim().isEmpty()) {
            return Result.error("用户姓名不能为空");
        }

        try {
            Map<String, Object> result = communityActivityService.registerActivity(
                    activityId, userId, userName, userPhone, remarks
            );
            return Result.success("报名成功", result);
        } catch (RuntimeException e) {
            log.error("报名失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消报名
     */
    @PostMapping("/community-activity/cancel")
    @Operation(summary = "取消报名", description = "用户取消活动报名，取消后活动人数自动-1")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "取消报名请求参数<br><b>参数说明：</b><br>" +
                "- <code>activityId</code>: 必填，活动ID<br>" +
                "- <code>userId</code>: 必填，用户ID<br>" +
                "- <code>cancelReason</code>: 可选，取消原因",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = "{\"activityId\":\"1234567890\",\"userId\":\"user123\",\"cancelReason\":\"临时有事无法参加\"}"
            )
        )
    )
    public Result<String> cancelRegistration(@RequestBody Map<String, String> request) {

        String activityId = request.get("activityId");
        String userId = request.get("userId");
        String cancelReason = request.get("cancelReason");

        // 参数校验
        if (activityId == null || activityId.trim().isEmpty()) {
            return Result.error("活动ID不能为空");
        }
        if (userId == null || userId.trim().isEmpty()) {
            return Result.error("用户ID不能为空");
        }

        try {
            communityActivityService.cancelRegistration(activityId, userId, cancelReason);
            return Result.success("取消报名成功");
        } catch (RuntimeException e) {
            log.error("取消报名失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    }
