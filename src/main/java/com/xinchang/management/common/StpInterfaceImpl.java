package com.xinchang.management.common;

import cn.dev33.satoken.stp.StpInterface;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserMapper sysUserMapper;

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

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }
}