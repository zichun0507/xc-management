package com.xinchang.management.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类，对应数据库 operation_log 表
 */
@TableName("operation_log")
@Data
@Schema(description = "操作日志实体", name = "OperationLog")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志主键ID", example = "1")
    private Long id;

    @Schema(description = "操作人ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作人姓名", example = "管理员")
    private String operatorName;

    @Schema(description = "操作模块（COMPANY/ROOM/EMPLOYEE等）", example = "COMPANY")
    private String module;

    @Schema(description = "操作类型（CREATE/UPDATE/DELETE等）", example = "CREATE")
    private String action;

    @Schema(description = "操作内容详情")
    private String content;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
}
