package com.aurora.starter.webmvc.exception;

import com.aurora.starter.webmvc.response.R;
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

/**
 * 全局异常处理器.
 *
 * <p>HTTP 状态码统一 200，业务结果通过 {@link R#getCode()} 表达。
 * {@link NoHandlerFoundException} 需开启 {@code spring.mvc.throw-exception-if-no-handler-found=true} 才能被捕获。</p>
 *
 * @author whb
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldError();
        String message = first != null
                ? first.getField() + " " + first.getDefaultMessage()
                : DefaultErrorCode.PARAM_INVALID.getMessage();
        log.warn("参数校验失败: {}", message);
        return R.error(DefaultErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        ConstraintViolation<?> first = e.getConstraintViolations().stream().findFirst().orElse(null);
        String message = first != null
                ? first.getPropertyPath() + " " + first.getMessage()
                : DefaultErrorCode.PARAM_INVALID.getMessage();
        log.warn("参数校验失败: {}", message);
        return R.error(DefaultErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        FieldError first = e.getBindingResult().getFieldError();
        String message = first != null
                ? first.getField() + " " + first.getDefaultMessage()
                : DefaultErrorCode.PARAM_INVALID.getMessage();
        log.warn("参数校验失败: {}", message);
        return R.error(DefaultErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.error(DefaultErrorCode.PARAM_INVALID.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String message = "缺少参数: " + e.getParameterName();
        log.warn("缺少参数: {}", e.getParameterName());
        return R.error(DefaultErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return R.error(DefaultErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Throwable.class)
    public R<Void> handleThrowable(Throwable e) {
        log.error("未处理的异常", e);
        return R.error(DefaultErrorCode.SERVER_ERROR);
    }
}
