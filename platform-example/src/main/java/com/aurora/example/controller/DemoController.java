package com.aurora.example.controller;

import com.aurora.starter.webmvc.domain.response.Result;
import com.aurora.starter.webmvc.exception.BizException;
import com.aurora.starter.webmvc.exception.DefaultBizCode;
import com.aurora.starter.webmvc.filter.trace.TraceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * webmvc 通用件功能演示.
 *
 * <p>4 个端点演示：业务异常、未处理异常、参数校验、TraceId 读取。</p>
 *
 * @author whb
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    /**
     * 演示业务异常：抛出 {@link BizException}，由 {@code GlobalExceptionHandler} 转为
     * {@code Result{code=404, msg="资源不存在"}}.
     */
    @GetMapping("/biz-error")
    public Result<Void> bizError() {
        throw new BizException(DefaultBizCode.NOT_FOUND);
    }

    /**
     * 演示兜底异常：抛出 RuntimeException，触发 {@code handleThrowable(Throwable)}，
     * 控制台打 error 全栈，返回 {@code Result{code=500, msg="服务器内部错误"}}.
     */
    @GetMapping("/server-error")
    public Result<Void> serverError() {
        throw new RuntimeException("boom");
    }

    /**
     * 演示参数校验：故意传空 name 或负数 age，触发 {@code MethodArgumentNotValidException}，
     * 返回 {@code Result{code=400, msg="name xxx"}}.
     */
    @PostMapping("/validate")
    public Result<DemoForm> validate(@Valid @RequestBody DemoForm form) {
        return Result.data(form);
    }

    /**
     * 演示 TraceId 透传：直接从 {@link TraceContext} 读，与响应 header X-Trace-Id 一致.
     */
    @GetMapping("/trace")
    public Result<String> trace() {
        return Result.data(TraceContext.get());
    }

    /**
     * 校验演示用 DTO.
     */
    @Data
    public static class DemoForm {

        @NotBlank(message = "不能为空")
        private String name;

        @Min(value = 0, message = "不能为负数")
        private Integer age;
    }
}
