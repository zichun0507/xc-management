package com.xinchang.management.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.Result;
import com.xinchang.management.log.entity.OperationLog;
import com.xinchang.management.log.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @GetMapping
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