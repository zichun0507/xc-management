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

/**
 * 认证业务逻辑服务层，提供用户登录、登出、密码修改及当前用户信息查询功能
 */
@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 用户登录验证，校验用户名密码及账号状态后生成token
     * @param username 用户名
     * @param password 明文密码
     * @return 包含token及用户信息的登录结果
     */
    public Map<String, Object> login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        // 校验用户名存在且密码匹配
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        // 校验账号是否被禁用
        if ("DISABLED".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }
        // 执行sa-token登录
        StpUtil.login(user.getId());
        writeLog(user.getId(), user.getRealName(), "AUTH", "LOGIN", "用户登录");

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("tokenName", StpUtil.getTokenName());
        data.put("user", userInfoMap(user));
        return data;
    }

    /**
     * 用户登出，清除sa-token会话
     */
    public void logout() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            writeLog(userId, user.getRealName(), "AUTH", "LOGOUT", "用户登出");
        }
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户的基本信息
     * @return 用户信息Map
     */
    public Map<String, Object> getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return userInfoMap(user);
    }

    /**
     * 修改当前用户密码，需验证原密码正确性
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String oldPassword, String newPassword) {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        // 校验原密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        writeLog(userId, user.getRealName(), "AUTH", "UPDATE", "密码修改");
    }

    /**
     * 将SysUser实体转换为前端所需的用户信息Map
     * @param user 系统用户实体
     * @return 包含用户关键信息的Map
     */
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
