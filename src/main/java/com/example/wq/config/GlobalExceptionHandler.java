package com.example.wq.config;

import com.example.wq.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器 - 简化但完整
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 忽略浏览器自动请求的资源（favicon.ico, Chrome DevTools 等）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        // 静默忽略，不记录日志
        String uri = request.getRequestURI();
        if (!uri.contains("favicon.ico") &&
            !uri.contains(".well-known") &&
            !uri.contains("chrome.devtools")) {
            log.warn("资源不存在: {}", uri);
        }
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("参数错误: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理参数类型不匹配异常（如：传字符串给Integer字段）
     */
    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleParameterTypeMismatch(Exception e, HttpServletRequest request) {
        log.warn("参数格式错误: {} - {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "参数格式错误");
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("接口不存在: {} {}", request.getMethod(), request.getRequestURI());
        return Result.error(404, "接口不存在");
    }

    /**
     * 兜底处理 - 所有未捕获的异常
     * 包括：NullPointerException, RuntimeException 等
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        // 记录完整错误堆栈
        log.error("系统异常: {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        // 开发环境：返回详细错误信息
        // 生产环境：返回通用错误，不泄露敏感信息
        String errorMsg = getErrorMessage(e);
        return Result.error(500, errorMsg);
    }

    /**
     * 根据环境返回错误信息
     */
    private String getErrorMessage(Exception e) {
        if (isDevelopment()) {
            // 开发环境：返回详细错误
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        } else {
            // 生产环境：返回通用错误
            return "系统内部错误";
        }
    }

    /**
     * 判断是否是开发环境
     */
    private boolean isDevelopment() {
        String env = System.getProperty("spring.profiles.active");
        return !"prod".equals(env);
    }
}
