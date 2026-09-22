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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * 文档模板管理控制器，提供模板的上传、删除和查询接口
 */
@RestController
@RequestMapping("/api/templates")
@Tag(name = "模板管理", description = "代办文档模板的上传、删除与查询接口")
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

    /**
     * 查询所有文档模板列表
     * @return 模板列表
     */
    @GetMapping
    @Operation(summary = "查询模板列表", description = "获取所有代办文档模板信息")
    public Result<List<DocTemplate>> list() {
        return Result.ok(docTemplateMapper.selectList(
                new LambdaQueryWrapper<DocTemplate>().orderByDesc(DocTemplate::getUploadedTime)));
    }

    /**
     * 上传新的文档模板（仅支持docx格式）
     * @param file 模板文件
     * @param templateName 模板名称
     * @param templateType 模板类型（DOCUMENT/APPOINTMENT）
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PostMapping
    @Operation(summary = "上传模板", description = "上传新的代办文档模板，仅支持.docx格式")
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

    /**
     * 删除指定模板（同时删除服务器上的文件）
     * @param id 模板ID
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板", description = "删除指定ID的模板文件及数据库记录")
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
