package com.xinchang.management.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.log.entity.OperationLog;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志数据访问层接口
 */
@Mapper
@Schema(description = "操作日志数据访问接口")
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
