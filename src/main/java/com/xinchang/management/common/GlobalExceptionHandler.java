package com.xinchang.management.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 全局异常处理器，统一捕获并处理各类异常，返回标准化的错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     * @param e 业务异常
     * @param response HTTP响应对象
     * @return 标准化错误结果
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        response.setStatus(Math.max(e.getCode(), 400));
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理未登录异常
     * @param e 未登录异常
     * @param response HTTP响应对象
     * @return 401未登录错误结果
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletResponse response) {
        response.setStatus(401);
        return Result.fail(401, "登录状态过期，请重新登录");
    }

    /**
     * 处理无权限异常
     * @param e 无权限异常
     * @param response HTTP响应对象
     * @return 403无权限错误结果
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e, HttpServletResponse response) {
        response.setStatus(403);
        return Result.fail(403, "无权限访问");
    }

    /**
     * 处理缺少请求参数异常
     * @param e 缺少参数异常
     * @return 400参数缺失错误结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(400, "缺少必要参数：" + e.getParameterName());
    }

    /**
     * 处理请求参数格式异常
     * @param e 参数格式异常
     * @return 400参数格式错误结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求参数格式错误，请检查 JSON 格式");
    }

    /**
     * 处理不支持的请求方法异常
     * @param e 请求方法不支持异常
     * @return 405方法不支持错误结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(405, "不支持的请求方法");
    }

    /**
     * 处理数据完整性冲突异常
     * @param e 数据完整性异常
     * @return 400数据冲突错误结果
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性冲突", e);
        return Result.fail(400, "数据冲突，请检查是否重复提交或存在关联数据");
    }

    /**
     * 处理文件上传大小超出限制异常
     * @param e 上传大小异常
     * @return 400文件过大错误结果
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return Result.fail(400, "上传文件大小超过限制");
    }

    /**
     * 处理其他未捕获的系统异常
     * @param e 系统异常
     * @return 500服务器内部异常结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "服务器内部异常，请联系管理员");
    }
}
