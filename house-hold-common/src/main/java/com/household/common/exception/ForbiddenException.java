package com.household.common.exception;

/**
 * 无权操作异常，对应 HTTP 403
 *
 * @author household
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
