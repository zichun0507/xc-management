package com.xinchang.management.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 楼层实体类，对应数据库 floor 表
 */
@TableName("floor")
@Data
@Schema(description = "楼层实体", name = "Floor")
public class Floor {

    @TableId(type = IdType.AUTO)
    @Schema(description = "楼层主键ID", example = "1")
    private Long id;

    @Schema(description = "所属楼栋ID", example = "1")
    private Long buildingId;

    @Schema(description = "楼层名称", example = "1F")
    private String name;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
