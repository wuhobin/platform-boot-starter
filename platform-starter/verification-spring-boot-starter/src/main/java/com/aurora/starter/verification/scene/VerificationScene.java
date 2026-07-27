package com.aurora.starter.verification.scene;

/**
 * 验证码业务场景。
 *
 * <p>下游可以让自己的枚举实现此接口，以扩展业务场景。</p>
 */
public interface VerificationScene {

    /**
     * 返回场景编码。
     *
     * @return 场景编码
     */
    String code();
}
