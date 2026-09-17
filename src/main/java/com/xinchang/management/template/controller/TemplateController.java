package com.xinchang.management.template.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.common.Result;
import com.xinchang.management.template.entity.DocTemplate;
import com.xinchang.management.template.mapper.DocTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    @Autowired
    private DocTemplateMapper docTemplateMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.upload.base-path:./uploads}")
    private String uploadBasePath;

    private static final Set<String> ALLOWED_FORMATS = Set.of("docx");
    private static final Set<String> EXECUTABLE_FORMATS = Set.of("exe", "bat", "sh", "dll", "com", "scr", "pif", "vbs", "msi", "reg", "cmd", "ps1");
    private static final long MAX_TEMPLATE_SIZE = 50 * 1024 * 1024;

    @GetMapping
    public Result<List<DocTemplate>> list() {
        return Result.ok(docTemplateMapper.selectList(
                new LambdaQueryWrapper<DocTemplate>().orderByDesc(DocTemplate::getUploadedTime)));
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public Result<Void> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam("templateName") String templateName,
                               @RequestParam(value = "templateType", defaultValue = "DOCUMENT") String templateType) {
        if (file.isEmpty()) throw new BusinessException("上传文件为空");
        if (!Set.of("DOCUMENT", "APPOINTMENT").contains(templateType)) {
            throw new BusinessException("无效的模板类型");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) throw new BusinessException("文件名不能为空");
        String ext = getExt(originalName).toLowerCase();
        if (EXECUTABLE_FORMATS.contains(ext)) {
            throw new BusinessException("禁止上传可执行文件");
        }
        if (!ALLOWED_FORMATS.contains(ext)) {
            throw new BusinessException("仅支持 .docx 格式的模板文件");
        }
        if (file.getSize() > MAX_TEMPLATE_SIZE) {
            throw new BusinessException("模板文件大小超出 50MB 限制");
        }

        String dir = uploadBasePath + "/templates/";
        ensureDir(dir);
        String storedName = UUID.randomUUID() + "." + ext;
        String fullPath = dir + storedName;
        try {
            Files.copy(file.getInputStream(), Paths.get(fullPath));
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }

        DocTemplate template = new DocTemplate();
        template.setTemplateName(templateName);
        template.setTemplateType(templateType);
        template.setStoredPath(fullPath);
        template.setOriginalName(originalName);
        template.setFileSize(file.getSize());
        template.setFileFormat(ext);
        template.setUploadedTime(LocalDateTime.now());
        docTemplateMapper.insert(template);
        writeLog("TEMPLATE", "UPLOAD", "上传模板：" + templateName);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DocTemplate template = docTemplateMapper.selectById(id);
        if (template == null) throw new BusinessException("模板不存在");
        try {
            Files.deleteIfExists(Paths.get(template.getStoredPath()));
        } catch (IOException ignored) {}
        docTemplateMapper.deleteById(id);
        writeLog("TEMPLATE", "DELETE", "删除模板：" + template.getTemplateName());
        return Result.ok();
    }

    private String getExt(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx == -1 ? "" : filename.substring(idx + 1);
    }

    private void ensureDir(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            throw new BusinessException("创建目录失败");
        }
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