package com.example.wq.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 配置 - 自定义日期时间格式、智能类型转换
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 智能 BigDecimal 反序列化器
     * 支持：String、Integer、Long、Double、Float、BigDecimal 等类型自动转换
     */
    public static class SmartBigDecimalDeserializer extends StdDeserializer<BigDecimal> {

        public SmartBigDecimalDeserializer() {
            super(BigDecimal.class);
        }

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // 根据当前 token 类型处理不同的值
            switch (p.currentToken()) {
                case VALUE_NULL:
                    return BigDecimal.ZERO;
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                    return p.getDecimalValue();
                case VALUE_STRING:
                    String value = p.getValueAsString();
                    if (value == null || value.trim().isEmpty()) {
                        return BigDecimal.ZERO;
                    }
                    try {
                        return new BigDecimal(value.trim());
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                default:
                    // 尝试作为字符串处理
                    try {
                        String strValue = p.getValueAsString();
                        if (strValue != null && !strValue.trim().isEmpty()) {
                            return new BigDecimal(strValue.trim());
                        }
                    } catch (Exception e) {
                        // 忽略异常
                    }
                    return BigDecimal.ZERO;
            }
        }
    }

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 注册 JavaTimeModule
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 自定义 LocalDateTime 序列化格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));

        // 注册智能 BigDecimal 反序列化器（通过 SimpleModule）
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(BigDecimal.class, new SmartBigDecimalDeserializer());

        objectMapper.registerModule(javaTimeModule);
        objectMapper.registerModule(simpleModule);

        // 注册 Hibernate6Module - 处理Hibernate代理对象序列化
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        // 配置Hibernate模块
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate6Module.configure(Hibernate6Module.Feature.WRITE_MISSING_ENTITIES_AS_NULL, true);
        // 替换代理对象而不是序列化代理类
        hibernate6Module.configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true);
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        objectMapper.registerModule(hibernate6Module);

        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
