package com.example.wq.annotation;

import java.lang.annotation.*;

/**
 * 排除字段注解
 *
 * 标记在实体字段上，表示该字段不需要通过反射接口返回
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcludeField {
}
