package com.aurora.starter.webmvc.domain.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.aurora.starter.webmvc.constants.HttpStatus;
import com.aurora.starter.webmvc.exception.BizCode;
import com.aurora.starter.webmvc.constants.TraceConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 接口统一返回对象.
 *
 * <p>HTTP 状态码统一返回 200，业务结果由 {@link #code} 字段承载（HTTP 风格：200 成功、4xx/5xx 错误）。</p>
 *
 * @param <T> data 类型
 * @author whb
 */
@Data
@NoArgsConstructor
@Schema(description = "接口统一返回对象")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = -5031098257303676979L;

    private static final String SUCCESS_MSG = "success";

    private static final String ERROR_MSG = "操作失败";

    @Schema(description = "状态码，200=成功")
    private int code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "数据")
    private T data;

    @Schema(description = "链路追踪 Id")
    private String traceId;

    @Schema(description = "额外信息")
    private Map<String, Object> extra = new LinkedHashMap<>();

    private Result(int code, String message) {
        this.code = code;
        this.message = message;
        this.traceId = MDC.get(TraceConstants.MDC_KEY);
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get(TraceConstants.MDC_KEY);
    }

    public Result<T> putExtra(String key, Object value) {
        this.extra.put(key, value);
        return this;
    }

    /**
     * @deprecated use the canonical {@code message} property.
     */
    @Deprecated(forRemoval = false)
    @JsonIgnore
    public String getMsg() {
        return message;
    }

    /**
     * @deprecated use the canonical {@code message} property.
     */
    @Deprecated(forRemoval = false)
    @JsonIgnore
    public void setMsg(String msg) {
        this.message = msg;
    }

    /**
     * 当前响应是否成功（code == 200）.
     */
    public boolean ok() {
        return HttpStatus.SUCCESS == this.code;
    }

    /**
     * 根据 boolean 结果返回成功或失败响应.
     */
    public static <T> Result<T> result(boolean ok) {
        return ok ? success() : error();
    }

    /**
     * 成功响应，使用默认提示 "操作成功".
     */
    public static <T> Result<T> success() {
        return success(SUCCESS_MSG);
    }

    /**
     * 成功响应，附带数据，使用默认提示 "操作成功".
     */
    public static <T> Result<T> data(T data) {
        return success(SUCCESS_MSG, data);
    }

    /**
     * 成功响应，自定义提示.
     */
    public static <T> Result<T> success(String msg) {
        return new Result<>(HttpStatus.SUCCESS, msg);
    }

    /**
     * 成功响应，自定义提示 + 数据.
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(HttpStatus.SUCCESS, msg, data);
    }

    /**
     * 失败响应，使用默认提示 "操作失败"（code=500）.
     */
    public static <T> Result<T> error() {
        return error(ERROR_MSG);
    }

    /**
     * 失败响应，自定义提示（code=500）.
     */
    public static <T> Result<T> error(String msg) {
        return error(HttpStatus.ERROR, msg);
    }

    /**
     * 失败响应，依据 {@link BizCode} 取 code 与默认 message.
     */
    public static <T> Result<T> error(BizCode bizCode) {
        return new Result<>(bizCode.getCode(), bizCode.getMessage());
    }

    /**
     * 失败响应，依据 {@link BizCode} 取 code，自定义 message.
     */
    public static <T> Result<T> error(BizCode bizCode, String msg) {
        return new Result<>(bizCode.getCode(), msg);
    }

    /**
     * 失败响应，自定义 code 与 message.
     */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg);
    }
}
