package com.xinchang.management.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人员附件实体类，对应数据库 attachment 表
 */
@TableName("attachment")
@Data
@Schema(description = "人员附件实体", name = "Attachment")
public class Attachment {

    @TableId(type = IdType.AUTO)
    @Schema(description = "附件主键ID", example = "1")
    private Long id;

    @Schema(description = "人员ID", example = "1")
    private Long employeeId;

    @Schema(description = "附件类型（ID_CARD_FRONT/ID_CARD_BACK/GRADUATION_CERT/EDUCATION_REPORT）", example = "ID_CARD_FRONT")
    private String attachmentType;

    @Schema(description = "文件原始名称", example = "身份证正反面.jpg")
    private String originalName;

    @Schema(description = "文件存储路径")
    private String storedPath;

    @Schema(description = "文件大小（字节）", example = "102400")
    private Long fileSize;

    @Schema(description = "文件格式", example = "jpg")
    private String fileFormat;

    @Schema(description = "上传时间")
    private LocalDateTime uploadedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
