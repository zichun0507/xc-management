package com.xinchang.management.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企业实体类，对应数据库 company 表
 */
@TableName("company")
@Data
@Schema(description = "企业实体", name = "Company")
public class Company {

    @TableId(type = IdType.AUTO)
    @Schema(description = "企业主键ID", example = "1")
    private Long id;

    @Schema(description = "企业全称", example = "新长科技有限公司")
    private String companyName;

    @Schema(description = "企业简称", example = "新长科技")
    private String shortName;

    @Schema(description = "企业英文名称", example = "XinChang Technology Co., Ltd.")
    private String englishName;

    @Schema(description = "统一社会信用代码", example = "91330100MA2KXXXXXX")
    private String unifiedCode;

    @Schema(description = "法定代表人", example = "张三")
    private String legalPerson;

    @Schema(description = "联系人", example = "李四")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800001111")
    private String contactPhone;

    @Schema(description = "经营地址", example = "浙江省杭州市余杭区XXX路1号")
    private String address;

    @Schema(description = "业务状态（NORMAL-正常运营/MOVED_OUT-迁出/SUSPENDED-停办）", example = "NORMAL")
    private String businessStatus;

    @Schema(description = "租约开始日期", example = "2024-01-01")
    private LocalDate leaseStartDate;

    @Schema(description = "租约到期日期", example = "2025-12-31")
    private LocalDate leaseEndDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
