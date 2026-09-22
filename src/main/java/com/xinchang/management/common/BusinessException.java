package com.xinchang.management.common;

/**
 * 业务异常类，用于封装业务层面的错误信息
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;

    /**
     * 构造业务异常（指定状态码和消息）
     * @param code 错误状态码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造业务异常（默认400状态码）
     * @param message 错误消息
     */
    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
