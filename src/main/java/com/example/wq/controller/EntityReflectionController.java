package com.example.wq.controller;

import com.example.wq.annotation.ExcludeField;
import com.example.wq.entity.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 实体反射控制器
 *
 * 通过反射获取实体字段信息
 */
@RestController
@RequestMapping("/api/entity")
@Tag(name = "实体反射", description = "通用实体字段查询接口")
public class EntityReflectionController {

    /**
     * 获取实体类字段元数据
     */
    @GetMapping("/fields/{className}")
    @Operation(summary = "获取类字段元数据", description = "通过类名获取字段信息，支持简短类名或完整类名，自动过滤标记了@ExcludeField的字段，枚举类型返回枚举值")
    public Result<Map<String, Object>> getClassFields(
        @Parameter(description = "类名（支持简短类名如WqUser或完整类名如com.example.wq.entity.WqUser）", example = "WqUser", required = true)
        @PathVariable String className) {

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Class<?> clazz = findClass(className);
            if (clazz == null) {
                return Result.error("类未找到: " + className + "，请使用完整类名或确保类在entity包下");
            }

            Field[] fields = getAllFields(clazz);

            for (Field field : fields) {
                // 检查是否有 @ExcludeField 注解
                if (field.isAnnotationPresent(ExcludeField.class)) {
                    continue;
                }

                Map<String, Object> fieldInfo = new LinkedHashMap<>();
                Class<?> fieldType = field.getType();

                fieldInfo.put("name", field.getName());
                fieldInfo.put("type", fieldType.getSimpleName());

                // 获取 @Schema 注解中的 description
                if (field.isAnnotationPresent(Schema.class)) {
                    Schema schema = field.getAnnotation(Schema.class);
                    String description = schema.description();
                    if (description != null && !description.isEmpty()) {
                        fieldInfo.put("description", description);
                    }
                }

                // 检查是否有对应的枚举方法（如 getGenderEnum）
                String enumMethodName = "get" + capitalize(field.getName()) + "Enum";
                try {
                    Method enumMethod = clazz.getMethod(enumMethodName);
                    Class<?> returnType = enumMethod.getReturnType();

                    // 如果返回类型是枚举
                    if (returnType.isEnum()) {
                        Map<String, String> enumValues = new LinkedHashMap<>();
                        Object[] enumConstants = returnType.getEnumConstants();

                        for (Object enumConstant : enumConstants) {
                            // 获取枚举的 code 和 description
                            try {
                                Method getCodeMethod = returnType.getMethod("getCode");
                                Method getDescMethod = returnType.getMethod("getDescription");

                                Object code = getCodeMethod.invoke(enumConstant);
                                Object desc = getDescMethod.invoke(enumConstant);

                                enumValues.put(String.valueOf(code), String.valueOf(desc));
                            } catch (Exception e) {
                                // 如果没有 getCode 和 getDescription 方法，使用 name()
                                enumValues.put(enumConstant.toString(), enumConstant.toString());
                            }
                        }

                        if (!enumValues.isEmpty()) {
                            fieldInfo.put("enumType", returnType.getSimpleName());
                            fieldInfo.put("enumValues", enumValues);
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // 没有枚举方法，忽略
                }

                // 如果字段本身就是枚举类型
                if (fieldType.isEnum()) {
                    Map<String, String> enumValues = new LinkedHashMap<>();
                    Object[] enumConstants = fieldType.getEnumConstants();

                    for (Object enumConstant : enumConstants) {
                        try {
                            Method getCodeMethod = fieldType.getMethod("getCode");
                            Method getDescMethod = fieldType.getMethod("getDescription");

                            Object code = getCodeMethod.invoke(enumConstant);
                            Object desc = getDescMethod.invoke(enumConstant);

                            enumValues.put(String.valueOf(code), String.valueOf(desc));
                        } catch (Exception e) {
                            enumValues.put(enumConstant.toString(), enumConstant.toString());
                        }
                    }

                    if (!enumValues.isEmpty()) {
                        fieldInfo.put("enumType", fieldType.getSimpleName());
                        fieldInfo.put("enumValues", enumValues);
                    }
                }

                result.put(field.getName(), fieldInfo);
            }

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取字段信息失败: " + e.getMessage());
        }
    }

    /**
     * 查找类（支持简短类名和完整类名）
     */
    private Class<?> findClass(String className) {
        try {
            // 先尝试直接加载（可能是完整类名）
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            // 如果失败，尝试在常见包路径下搜索
            String[] basePackages = {
                "com.example.wq.entity",
                "com.example.wq.enums",
                "com.example.wq.dto",
                "com.example.wq.vo"
            };

            for (String pkg : basePackages) {
                try {
                    return Class.forName(pkg + "." + className);
                } catch (ClassNotFoundException ignored) {
                    // 继续尝试下一个包
                }
            }

            return null;
        }
    }

    /**
     * 递归获取类及其父类的所有字段
     */
    private Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }

        return fields.toArray(new Field[0]);
    }

    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
