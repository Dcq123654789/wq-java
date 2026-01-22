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

import java.util.List;
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
        description = "统一的实体CRUD操作接口，支持创建、查询、更新、删除操作。所有实体都通过此接口进行操作。"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "请求体参数",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @ExampleObject(
                    name = "创建用户示例",
                    summary = "创建新用户",
                    value = "{\n" +
                           "  \"entity\": \"user\",\n" +
                           "  \"action\": \"create\",\n" +
                           "  \"data\": {\n" +
                           "    \"username\": \"zhangsan\",\n" +
                           "    \"password\": \"123456\",\n" +
                           "    \"email\": \"zhangsan@example.com\",\n" +
                           "    \"phone\": \"13800138000\",\n" +
                           "    \"status\": 1,\n" +
                           "    \"role\": \"user\"\n" +
                           "  }\n" +
                           "}"
                ),
                @ExampleObject(
                    name = "查询用户示例",
                    summary = "条件查询用户",
                    value = "{\n" +
                           "  \"entity\": \"user\",\n" +
                           "  \"action\": \"query\",\n" +
                           "  \"conditions\": {\n" +
                           "    \"status\": 1,\n" +
                           "    \"role\": \"user\"\n" +
                           "  },\n" +
                           "  \"pageNum\": 1,\n" +
                           "  \"pageSize\": 10,\n" +
                           "  \"sort\": {\n" +
                           "    \"createTime\": \"desc\"\n" +
                           "  }\n" +
                           "}"
                ),
                @ExampleObject(
                    name = "更新用户示例",
                    summary = "更新用户信息",
                    value = "{\n" +
                           "  \"entity\": \"user\",\n" +
                           "  \"action\": \"update\",\n" +
                           "  \"id\": \"1703123456789_1234\",\n" +
                           "  \"data\": {\n" +
                           "    \"email\": \"zhangsan_new@example.com\",\n" +
                           "    \"phone\": \"13900139000\"\n" +
                           "  }\n" +
                           "}"
                ),
                @ExampleObject(
                    name = "删除用户示例",
                    summary = "删除用户",
                    value = "{\n" +
                           "  \"entity\": \"user\",\n" +
                           "  \"action\": \"delete\",\n" +
                           "  \"id\": \"1703123456789_1234\"\n" +
                           "}"
                ),
                @ExampleObject(
                    name = "复杂查询示例",
                    summary = "使用MongoDB风格的复杂查询条件",
                    value = "{\n" +
                           "  \"entity\": \"user\",\n" +
                           "  \"action\": \"query\",\n" +
                           "  \"conditions\": {\n" +
                           "    \"$and\": [\n" +
                           "      {\"status\": 1},\n" +
                           "      {\"$or\": [\n" +
                           "        {\"username\": {\"$like\": \"zhang\"}},\n" +
                           "        {\"email\": {\"$like\": \"@example.com\"}}\n" +
                           "      ]},\n" +
                           "      {\"createTime\": {\"$gte\": \"2023-01-01 00:00:00\"}}\n" +
                           "    ]\n" +
                           "  },\n" +
                           "  \"pageNum\": 1,\n" +
                           "  \"pageSize\": 20\n" +
                           "}"
                )
            }
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
        // 1. 解析基础参数
        String entity = String.valueOf(payload.getOrDefault("entity", ""));
        String action = String.valueOf(payload.getOrDefault("action", ""));

        // 2. 参数校验
        if (entity.isEmpty() || action.isEmpty()) {
            return Result.error("entity和action参数不能为空");
        }

        // 3. 根据操作类型路由到对应方法
        switch (action.toLowerCase()) {
            case "create":
                return handleCreate(entity, payload);
            case "query":
                return handleQuery(entity, payload);
            case "update":
                return handleUpdate(entity, payload);
            case "delete":
                return handleDelete(entity, payload);
            default:
                return Result.error("不支持的操作类型: " + action);
        }
    }

    /**
     * 处理创建操作
     */
    private Result<?> handleCreate(String entity, Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) {
            return Result.error("创建操作需要提供data参数");
        }
        return userService.createByEntityName(entity, data);
    }

    /**
     * 处理查询操作
     */
    private Result<?> handleQuery(String entity, Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> conditions = (Map<String, Object>) payload.get("conditions");
        @SuppressWarnings("unchecked")
        Map<String, Object> sort = (Map<String, Object>) payload.get("sort");
        @SuppressWarnings("unchecked")
        List<String> fetch = (List<String>) payload.get("fetch");

        Integer pageNum = payload.get("pageNum") != null ?
            Integer.parseInt(String.valueOf(payload.get("pageNum"))) : null;
        Integer pageSize = payload.get("pageSize") != null ?
            Integer.parseInt(String.valueOf(payload.get("pageSize"))) : null;

        return userService.queryByEntityName(entity, conditions, pageNum, pageSize, sort, fetch);
    }

    /**
     * 处理更新操作
     */
    private Result<?> handleUpdate(String entity, Map<String, Object> payload) {
        String id = String.valueOf(payload.getOrDefault("id", ""));
        if (id.isEmpty()) {
            return Result.error("更新操作需要提供id参数");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) {
            return Result.error("更新操作需要提供data参数");
        }

        return userService.updateByEntityName(entity, id, data);
    }

    /**
     * 处理删除操作
     */
    private Result<?> handleDelete(String entity, Map<String, Object> payload) {
        String id = String.valueOf(payload.getOrDefault("id", ""));
        if (id.isEmpty()) {
            return Result.error("删除操作需要提供id参数");
        }

        return userService.deleteByEntityName(entity, id);
    }
}