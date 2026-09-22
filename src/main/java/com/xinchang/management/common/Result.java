package com.xinchang.management.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应结果封装类
 * @param <T> 响应数据类型
 */
@Data
@Schema(description = "统一响应结果")
public class Result<T> {

    @Schema(description = "响应状态码", example = "200")
    private int code;

    @Schema(description = "响应消息", example = "success")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    private Result() {
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造成功响应（携带数据）
     * @param data 响应数据
     * @param <T> 泛型类型
     * @return 统一响应结果
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 构造成功响应（无数据）
     * @param <T> 泛型类型
     * @return 统一响应结果
     */
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    /**
     * 构造失败响应（自定义状态码和消息）
     * @param code 状态码
     * @param message 错误消息
     * @param <T> 泛型类型
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 构造失败响应（默认500状态码）
     * @param message 错误消息
     * @param <T> 泛型类型
     * @return 统一响应结果
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}
