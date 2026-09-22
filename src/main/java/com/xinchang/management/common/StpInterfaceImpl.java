package com.xinchang.management.common;

import cn.dev33.satoken.stp.StpInterface;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * sa-token 权限接口实现类，提供用户角色列表和权限列表的查询功能
 */
@Component
@Schema(description = "sa-token权限接口实现")
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 获取登录用户的角色列表
     * @param loginId 登录用户ID
     * @param loginType 登录类型
     * @return 用户角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId;
        if (loginId instanceof Number) {
            userId = ((Number) loginId).longValue();
        } else {
            userId = Long.parseLong(loginId.toString());
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRole());
    }

    /**
     * 获取登录用户的权限列表（当前实现返回空列表）
     * @param loginId 登录用户ID
     * @param loginType 登录类型
     * @return 权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}
