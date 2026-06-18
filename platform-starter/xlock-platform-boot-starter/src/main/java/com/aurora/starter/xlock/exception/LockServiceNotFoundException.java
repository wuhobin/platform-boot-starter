package com.aurora.starter.xlock.exception;

/**
 * 没有找到相应的锁服务实现类.
 *
 * @author whb
 */
public class LockServiceNotFoundException extends LockException {

    private static final long serialVersionUID = -8199483743071016533L;

    public LockServiceNotFoundException() {
        super("没有找到相应的锁服务实现类");
    }

}
