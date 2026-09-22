package com.xinchang.management.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业人员实体类，对应数据库 employee 表
 */
@TableName("employee")
@Data
@Schema(description = "企业人员实体", name = "Employee")
public class Employee {

    @TableId(type = IdType.AUTO)
    @Schema(description = "人员主键ID", example = "1")
    private Long id;

    @Schema(description = "所属企业ID", example = "1")
    private Long companyId;

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "性别", example = "男")
    private String gender;

    @Schema(description = "身份证号", example = "33010119900101XXXX")
    private String idCard;

    @Schema(description = "毕业学校")
    private String school;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "联系电话", example = "13800001111")
    private String phone;

    @Schema(description = "照片存储路径")
    private String photo;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "人员状态（ACTIVE-在职/INACTIVE-停办）", example = "ACTIVE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
