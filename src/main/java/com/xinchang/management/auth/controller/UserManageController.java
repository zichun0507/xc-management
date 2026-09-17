package com.xinchang.management.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SaCheckRole("ADMIN")
@RestController
@RequestMapping("/api/users")
public class UserManageController {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return Result.ok(sysUserMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreatedTime)));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> create(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        String role = body.get("role");

        if (username == null || username.isBlank()) throw new BusinessException("用户名不能为空");
        if (password == null || password.isBlank()) throw new BusinessException("密码不能为空");
        if (role == null || (!role.equals("ADMIN") && !role.equals("USER"))) {
            throw new BusinessException("角色值无效");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setStatus("ENABLED");
        sysUserMapper.insert(user);
        writeLog("USER", "CREATE", "新增用户：" + username);
        return Result.ok();
    }

    @PutMapping("/{id}/role")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || (!role.equals("ADMIN") && !role.equals("USER"))) {
            throw new BusinessException("角色值无效");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setRole(role);
        sysUserMapper.updateById(user);
        writeLog("USER", "UPDATE", "修改用户角色：" + user.getUsername() + " → " + role);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isBlank()) throw new BusinessException("密码不能为空");
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        writeLog("USER", "UPDATE", "重置用户密码：" + user.getUsername());
        return Result.ok();
    }

    private void writeLog(String module, String action, String content) {
        long operatorId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(operatorId);
        String operatorName = user != null ? user.getRealName() : String.valueOf(operatorId);
        jdbcTemplate.update(
                "INSERT INTO operation_log (operator_id, operator_name, module, action, content) VALUES (?, ?, ?, ?, ?)",
                operatorId, operatorName, module, action, content);
    }
}