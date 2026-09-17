package com.xinchang.management.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        StpUtil.login(user.getId());
        writeLog(user.getId(), user.getRealName(), "AUTH", "LOGIN", "用户登录");

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("tokenName", StpUtil.getTokenName());
        data.put("user", userInfoMap(user));
        return data;
    }

    public void logout() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            writeLog(userId, user.getRealName(), "AUTH", "LOGOUT", "用户登出");
        }
        StpUtil.logout();
    }

    public Map<String, Object> getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return userInfoMap(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String oldPassword, String newPassword) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        writeLog(userId, user.getRealName(), "AUTH", "UPDATE", "密码修改");
    }

    private Map<String, Object> userInfoMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("role", user.getRole());
        map.put("status", user.getStatus());
        return map;
    }

    private void writeLog(Long operatorId, String operatorName, String module, String action, String content) {
        jdbcTemplate.update(
                "INSERT INTO operation_log (operator_id, operator_name, module, action, content) VALUES (?, ?, ?, ?, ?)",
                operatorId, operatorName, module, action, content);
    }
}