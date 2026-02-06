package com.example.wq.controller;

import com.example.wq.entity.Result;
import com.example.wq.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通用批处理控制器 - 统一CRUD接口
 */
@RestController
@RequestMapping("/api/batch")
@Tag(name = "通用CRUD接口", description = "基于Hibernate的统一实体CRUD操作接口")
public class BatchController {

    @Autowired
    private UserService userService;

    /**
     * 通用批处理接口
     * 支持 create, query, update, delete 操作
     */
    @PostMapping
    @Operation(
        summary = "通用CRUD操作接口",
        description = "<b>支持的操作类型：</b><br>" +
                   "- <code>create</code>: 创建实体<br>" +
                   "- <code>query</code>: 查询实体<br>" +
                   "- <code>update</code>: 更新实体<br>" +
                   "- <code>delete</code>: 删除实体<br><br>" +
                   "<b>通用参数：</b><br>" +
                   "- <code>entity</code>: 必填，实体名称(如: wquser)<br>" +
                   "- <code>action</code>: 必填，操作类型(create/query/update/delete)<br><br>" +
                   "<b>请求示例：</b><br>" +
                   "查询：{&quot;entity&quot;:&quot;wquser&quot;,&quot;action&quot;:&quot;query&quot;,&quot;pageNum&quot;:1,&quot;pageSize&quot;:10}"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "通用CRUD请求参数，在下方编辑JSON格式的请求体",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = "{\"entity\":\"wquser\",\"action\":\"query\",\"pageNum\":1,\"pageSize\":10,\"sort\":{\"createTime\":\"desc\"}}"
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "操作成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Result.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Result.class)
            )
        ),
        @ApiResponse(responseCode = "500", description = "服务器内部错误",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Result.class)
            )
        )
    })
    public Result<?> batch(@RequestBody Map<String, Object> payload) {
        // 1. 参数校验
        String entity = String.valueOf(payload.getOrDefault("entity", ""));
        String action = String.valueOf(payload.getOrDefault("action", ""));

        if (entity.isEmpty()) {
            return Result.error("entity参数不能为空");
        }
        if (action.isEmpty()) {
            return Result.error("action参数不能为空");
        }

        // 2. 根据操作类型路由到对应方法
        switch (action.toLowerCase()) {
            case "create":
                return handleCreate(payload);
            case "query":
                return handleQuery(payload);
            case "update":
                return handleUpdate(payload);
            case "delete":
                return handleDelete(payload);
            default:
                return Result.error("不支持的操作类型: " + action);
        }
    }

    /**
     * 处理创建操作
     */
    private Result<?> handleCreate(Map<String, Object> payload) {
        String entity = String.valueOf(payload.getOrDefault("entity", ""));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");

        if (data == null || data.isEmpty()) {
            return Result.error("创建操作需要提供data参数");
        }
        return userService.createByEntityName(entity, data);
    }

    /**
     * 处理查询操作
     */
    private Result<?> handleQuery(Map<String, Object> payload) {
        String entity = String.valueOf(payload.getOrDefault("entity", ""));
        @SuppressWarnings("unchecked")
        Map<String, Object> conditions = (Map<String, Object>) payload.get("conditions");
        @SuppressWarnings("unchecked")
        Map<String, Object> sort = (Map<String, Object>) payload.get("sort");
        @SuppressWarnings("unchecked")
        java.util.List<String> fetch = (java.util.List<String>) payload.get("fetch");

        Integer pageNum = payload.get("pageNum") != null ?
            Integer.parseInt(String.valueOf(payload.get("pageNum"))) : null;
        Integer pageSize = payload.get("pageSize") != null ?
            Integer.parseInt(String.valueOf(payload.get("pageSize"))) : null;

        return userService.queryByEntityName(entity, conditions, pageNum, pageSize, sort, fetch);
    }

    /**
     * 处理更新操作
     */
    private Result<?> handleUpdate(Map<String, Object> payload) {
        String entity = String.valueOf(payload.getOrDefault("entity", ""));
        String id = String.valueOf(payload.getOrDefault("id", ""));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");

        if (id.isEmpty()) {
            return Result.error("更新操作需要提供id参数");
        }
        if (data == null || data.isEmpty()) {
            return Result.error("更新操作需要提供data参数");
        }
        return userService.updateByEntityName(entity, id, data);
    }

    /**
     * 处理删除操作
     */
    private Result<?> handleDelete(Map<String, Object> payload) {
        String entity = String.valueOf(payload.getOrDefault("entity", ""));
        String id = String.valueOf(payload.getOrDefault("id", ""));

        if (id.isEmpty()) {
            return Result.error("删除操作需要提供id参数");
        }
        return userService.deleteByEntityName(entity, id);
    }

}
