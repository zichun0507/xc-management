package com.xinchang.management.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.auth.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户数据访问层接口
 */
@Mapper
@Schema(description = "系统用户数据访问接口")
public interface SysUserMapper extends BaseMapper<SysUser> {
}
