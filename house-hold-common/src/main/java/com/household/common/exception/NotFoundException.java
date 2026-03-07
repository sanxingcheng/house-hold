package com.household.common.exception;

/**
 * 资源未找到异常，对应 HTTP 404
 *
 * @author household
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
