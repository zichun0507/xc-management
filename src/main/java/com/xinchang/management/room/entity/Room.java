package com.xinchang.management.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 房间实体类，对应数据库 room 表
 */
@TableName("room")
@Data
@Schema(description = "房间实体", name = "Room")
public class Room {

    @TableId(type = IdType.AUTO)
    @Schema(description = "房间主键ID", example = "1")
    private Long id;

    @Schema(description = "所属楼栋ID", example = "1")
    private Long buildingId;

    @Schema(description = "所属楼层ID", example = "1")
    private Long floorId;

    @Schema(description = "房间号", example = "101")
    private String roomNumber;

    @Schema(description = "房间状态（FREE-空闲/OCCUPIED-占用/DISABLED-停用）", example = "FREE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
