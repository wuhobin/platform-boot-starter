package com.aurora.starter.webmvc.exception;

import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.starter.webmvc.enums.DefaultBizCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器.
 *
 * <p>HTTP 状态码统一 200，业务结果通过 {@link Result} 的 {@code code} 字段表达。
 * {@link NoHandlerFoundException} 需开启 {@code spring.mvc.throw-exception-if-no-handler-found=true} 才能被捕获。</p>
 *
 * @author whb
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldError();
        String message = first != null
                ? first.getField() + " " + first.getDefaultMessage()
                : DefaultBizCode.PARAM_INVALID.getMessage();
        log.warn("参数校验失败: {}", message);
        return Result.error(DefaultBizCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        ConstraintViolation<?> first = e.getConstraintViolations().stream().findFirst().orElse(null);
        String message = first != null
                ? first.getPropertyPath() + " " + first.getMessage()
                : DefaultBizCode.PARAM_INVALID.getMessage();
        log.warn("参数校验失败: {}", message);
        return Result.error(DefaultBizCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError first = e.getBindingResult().getFieldError();
        String message = first != null
                ? first.getField() + " " + first.getDefaultMessage()
                : DefaultBizCode.PARAM_INVALID.getMessage();
        log.warn("参数校验失败: {}", message);
        return Result.error(DefaultBizCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error(DefaultBizCode.PARAM_INVALID, "请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String message = "缺少参数: " + e.getParameterName();
        log.warn("缺少参数: {}", e.getParameterName());
        return Result.error(DefaultBizCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.error(DefaultBizCode.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        // favicon.ico 等浏览器自动请求的静态资源，静默返回 404，不打日志
        return Result.error(DefaultBizCode.NOT_FOUND);
    }

    @ExceptionHandler(Throwable.class)
    public Result<Void> handleThrowable(Throwable e) {
        log.error("未处理的异常", e);
        return Result.error(DefaultBizCode.SERVER_ERROR);
    }
}
