package com.xinchang.management.auth.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器，提供用户的查询、创建、角色及密码管理接口
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "系统用户的管理接口，包括用户列表查询、创建、角色与密码管理")
public class UserManageController {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 分页查询系统用户列表
     * @param page 页码
     * @param size 每页条数
     * @return 分页用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询用户列表", description = "按创建时间倒序查询所有系统用户")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return Result.ok(sysUserMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>().orderByDesc(SysUser::getCreatedTime)));
    }

    /**
     * 新增系统用户（管理员角色限制为ADMIN或USER）
     * @param body 包含用户名、密码、真实姓名、角色的请求体
     * @return 操作结果
     */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "新增用户", description = "创建新系统用户，用户名不可重复，角色限ADMIN或USER")
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

    /**
     * 修改指定用户的角色
     * @param id 用户ID
     * @param body 包含新角色的请求体
     * @return 操作结果
     */
    @PutMapping("/{id}/role")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "修改用户角色", description = "修改指定用户的角色信息（限ADMIN或USER）")
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

    /**
     * 重置指定用户的密码
     * @param id 用户ID
     * @param body 包含新密码的请求体
     * @return 操作结果
     */
    @PutMapping("/{id}/password")
    @Transactional(rollbackFor = Exception.class)
    @Operation(summary = "重置用户密码", description = "将指定用户的密码重置为新密码")
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
