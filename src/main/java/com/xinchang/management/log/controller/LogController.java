package com.xinchang.management.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.Result;
import com.xinchang.management.log.entity.OperationLog;
import com.xinchang.management.log.mapper.OperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志控制器，提供操作日志的分页查询接口
 */
@RestController
@RequestMapping("/api/logs")
@Tag(name = "操作日志", description = "系统操作日志的查询与管理接口")
public class LogController {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 分页查询操作日志，支持按操作人、模块、时间范围筛选
     * @param operatorName 操作人姓名（可选）
     * @param module 操作模块（可选）
     * @param startTime 开始时间（可选，格式yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（可选，格式yyyy-MM-dd HH:mm:ss）
     * @param page 页码
     * @param size 每页条数
     * @return 分页操作日志列表
     */
    @GetMapping
    @Operation(summary = "分页查询操作日志", description = "支持按操作人、模块、时间范围筛选操作日志")
    public Result<Page<OperationLog>> page(
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.isNotBlank(operatorName), OperationLog::getOperatorName, operatorName)
                .eq(StringUtils.isNotBlank(module), OperationLog::getModule, module);
        if (StringUtils.isNotBlank(startTime)) {
            wrapper.ge(OperationLog::getOperationTime, LocalDateTime.parse(startTime));
        }
        if (StringUtils.isNotBlank(endTime)) {
            wrapper.le(OperationLog::getOperationTime, LocalDateTime.parse(endTime));
        }
        wrapper.orderByDesc(OperationLog::getOperationTime);
        return Result.ok(operationLogMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
