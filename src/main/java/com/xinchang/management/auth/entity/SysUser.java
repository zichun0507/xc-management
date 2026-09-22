package com.xinchang.management.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类，对应数据库 sys_user 表
 */
@TableName("sys_user")
@Data
@Schema(description = "系统用户实体", name = "SysUser")
public class SysUser {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户主键ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "加密后的密码")
    private String password;

    @Schema(description = "真实姓名", example = "管理员")
    private String realName;

    @Schema(description = "角色（ADMIN-管理员/USER-普通用户）", example = "ADMIN")
    private String role;

    @Schema(description = "用户状态（ENABLED-启用/DISABLED-禁用）", example = "ENABLED")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
