package com.xinchang.management.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档模板实体类，对应数据库 doc_template 表
 */
@TableName("doc_template")
@Data
@Schema(description = "文档模板实体", name = "DocTemplate")
public class DocTemplate {

    @TableId(type = IdType.AUTO)
    @Schema(description = "模板主键ID", example = "1")
    private Long id;

    @Schema(description = "模板名称", example = "入驻代办文档模板")
    private String templateName;

    @Schema(description = "模板类型（DOCUMENT-代办文档/APPOINTMENT-任命书）", example = "DOCUMENT")
    private String templateType;

    @Schema(description = "模板文件存储路径")
    private String storedPath;

    @Schema(description = "模板文件原始名称", example = "template.docx")
    private String originalName;

    @Schema(description = "文件大小（字节）", example = "20480")
    private Long fileSize;

    @Schema(description = "文件格式", example = "docx")
    private String fileFormat;

    @Schema(description = "上传时间")
    private LocalDateTime uploadedTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
