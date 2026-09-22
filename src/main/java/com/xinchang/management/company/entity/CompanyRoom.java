package com.xinchang.management.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业-房间关联实体类，对应数据库 company_room 表
 */
@TableName("company_room")
@Data
@Schema(description = "企业房间关联实体", name = "CompanyRoom")
public class CompanyRoom {

    @TableId(type = IdType.AUTO)
    @Schema(description = "关联主键ID", example = "1")
    private Long id;

    @Schema(description = "企业ID", example = "1")
    private Long companyId;

    @Schema(description = "房间ID", example = "10")
    private Long roomId;

    @Schema(description = "分配时间", example = "2024-01-15 10:00:00")
    private LocalDateTime allocatedTime;

    @Schema(description = "释放时间（迁出或停办时记录）")
    private LocalDateTime releasedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
