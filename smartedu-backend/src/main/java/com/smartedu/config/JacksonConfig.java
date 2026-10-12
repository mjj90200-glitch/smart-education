package com.smartedu.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

import static java.time.temporal.ChronoField.*;

/**
 * Jackson 序列化配置
 * 配置日期时间格式，支持多种格式输入
 */
@Configuration
public class JacksonConfig {

    // 定义支持的日期时间格式
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    // 支持 ISO 格式 (yyyy-MM-dd'T'HH:mm:ss) 和空格格式 (yyyy-MM-dd HH:mm:ss)
    private static final DateTimeFormatter FLEXIBLE_LOCAL_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd")
        .optionalStart()
        .appendLiteral('T')
        .optionalEnd()
        .optionalStart()
        .appendLiteral(' ')
        .optionalEnd()
        .appendPattern("HH:mm:ss")
        .toFormatter();

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            // 配置 LocalDateTime 序列化器（输出为空格格式）和反序列化器（支持 ISO 和空格格式）
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            builder.deserializerByType(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());

            // 配置默认时区
            builder.timeZone("GMT+8");
        };
    }

    // 自定义反序列化器，支持多种格式
    static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getText();
            try {
                return LocalDateTime.parse(dateStr, FLEXIBLE_LOCAL_DATE_TIME_FORMATTER);
            } catch (Exception e) {
                // 如果解析失败，尝试默认格式
                return LocalDateTime.parse(dateStr);
            }
        }
    }
}
