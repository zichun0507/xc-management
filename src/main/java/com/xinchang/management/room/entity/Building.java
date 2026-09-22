package com.xinchang.management.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼栋实体类，对应数据库 building 表
 */
@TableName("building")
@Data
@Schema(description = "楼栋实体", name = "Building")
public class Building {

    @TableId(type = IdType.AUTO)
    @Schema(description = "楼栋主键ID", example = "1")
    private Long id;

    @Schema(description = "楼栋名称", example = "A栋")
    private String name;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "业主/房东")
    private String landlord;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
