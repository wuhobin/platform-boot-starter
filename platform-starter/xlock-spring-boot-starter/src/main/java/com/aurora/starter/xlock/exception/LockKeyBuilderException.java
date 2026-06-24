package com.aurora.starter.xlock.exception;

/**
 * key构建异常.
 *
 * @author whb
 */
public class LockKeyBuilderException extends LockException {

    private static final long serialVersionUID = 713051615398843448L;

    public LockKeyBuilderException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LockKeyBuilderException(final String message) {
        super(message);
    }
}
