package com.aurora.starter.xlock.exception;

/**
 * 锁异常.
 *
 * @author whb
 */
public class LockException extends RuntimeException {

    public LockException(final String ex) {
        super(ex);
    }

    public LockException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
