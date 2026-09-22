package com.xinchang.management.template.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.common.Result;
import com.xinchang.management.company.entity.Company;
import com.xinchang.management.company.mapper.CompanyMapper;
import com.xinchang.management.employee.entity.Employee;
import com.xinchang.management.employee.mapper.EmployeeMapper;
import com.xinchang.management.template.entity.DocTemplate;
import com.xinchang.management.template.mapper.DocTemplateMapper;
import com.xinchang.management.template.service.DocRenderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 资料导出控制器，提供企业代办资料批量导出和任命书导出接口
 */
@RestController
@RequestMapping("/api/export")
@Tag(name = "资料导出", description = "企业代办资料打包导出及任命书导出接口")
public class ExportController {

    @Autowired
    private DocRenderService docRenderService;
    @Autowired
    private DocTemplateMapper docTemplateMapper;
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 导出企业所有代办资料为Zip压缩包（根据模板类型批量生成文档）
     * @param body 包含企业ID的请求体
     * @return Zip压缩包字节数组
     */
    @PostMapping("/company")
    @Operation(summary = "导出企业代办资料", description = "根据企业ID和所有DOCUMENT类型模板，批量生成代办文档并打包为Zip下载")
    public ResponseEntity<byte[]> exportCompanyDocs(@RequestBody Map<String, Long> body) {
        Long companyId = body.get("companyId");
        if (companyId == null) throw new BusinessException("企业ID不能为空");

        Company company = companyMapper.selectById(companyId);
        if (company == null) throw new BusinessException("企业不存在");

        List<DocTemplate> templates = docTemplateMapper.selectList(
                new LambdaQueryWrapper<DocTemplate>()
                        .eq(DocTemplate::getTemplateType, "DOCUMENT")
                        .orderByDesc(DocTemplate::getUploadedTime));
        if (templates.isEmpty()) throw new BusinessException("暂无代办模板");

        Map<String, String> companyFields = docRenderService.buildCompanyFieldMap(company);
        List<Employee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getCompanyId, companyId)
                        .eq(Employee::getStatus, "ACTIVE"));

        byte[] zipData = buildCompanyZip(company, templates, companyFields, employees);
        String filename = company.getCompanyName() + "_代办资料.zip";
        writeLog("EXPORT", "COMPANY", "导出企业代办资料：" + company.getCompanyName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodeFilename(filename))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipData);
    }

    /**
     * 导出指定人员的任命书为Zip压缩包
     * @param body 包含企业ID、人员ID列表及模板ID的请求体
     * @return Zip压缩包字节数组
     */
    @PostMapping("/appointment")
    @Operation(summary = "导出任命书", description = "根据企业ID和人员ID列表，批量生成任命书文档并打包为Zip下载")
    public ResponseEntity<byte[]> exportAppointment(@RequestBody Map<String, Object> body) {
        Long companyId = body.get("companyId") != null ? Long.valueOf(body.get("companyId").toString()) : null;
        List<Integer> empIdsRaw = (List<Integer>) body.get("employeeIds");
        Long templateId = body.get("templateId") != null ? Long.valueOf(body.get("templateId").toString()) : null;

        if (companyId == null || empIdsRaw == null || empIdsRaw.isEmpty()) {
            throw new BusinessException("企业ID和人员ID列表不能为空");
        }

        Company company = companyMapper.selectById(companyId);
        if (company == null) throw new BusinessException("企业不存在");

        DocTemplate template;
        if (templateId != null) {
            template = docTemplateMapper.selectById(templateId);
            if (template == null) throw new BusinessException("模板不存在");
        } else {
            template = docTemplateMapper.selectOne(
                    new LambdaQueryWrapper<DocTemplate>()
                            .eq(DocTemplate::getTemplateType, "APPOINTMENT")
                            .last("LIMIT 1"));
            if (template == null) throw new BusinessException("未找到任命书模板，请先上传");
        }

        List<Long> empIds = empIdsRaw.stream().map(Long::valueOf).toList();
        List<Employee> employees = employeeMapper.selectBatchIds(empIds);
        if (employees.isEmpty()) throw new BusinessException("未找到指定人员");

        Map<String, String> companyFields = docRenderService.buildCompanyFieldMap(company);
        byte[] zipData = buildAppointmentZip(company, template, companyFields, employees);
        String filename = company.getCompanyName() + "_任命书.zip";
        writeLog("EXPORT", "APPOINTMENT", "导出任命书：" + company.getCompanyName() + "，" + employees.size() + "人");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodeFilename(filename))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipData);
    }

    private byte[] buildCompanyZip(Company company, List<DocTemplate> templates,
                                       Map<String, String> companyFields, List<Employee> employees) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Map<String, String> employeeFields = employees.isEmpty()
                    ? new HashMap<>()
                    : docRenderService.buildEmployeeFieldMap(employees.get(0));

            Map<String, String> merged = new HashMap<>();
            merged.putAll(companyFields);
            merged.putAll(employeeFields);

            for (DocTemplate tpl : templates) {
                byte[] docData = docRenderService.render(tpl.getStoredPath(), merged);
                String entryName = tpl.getTemplateName() + ".docx";
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(docData);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出失败");
        }
    }

    private byte[] buildAppointmentZip(Company company, DocTemplate template,
                                          Map<String, String> companyFields, List<Employee> employees) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Employee emp : employees) {
                Map<String, String> merged = new HashMap<>();
                merged.putAll(companyFields);
                merged.putAll(docRenderService.buildEmployeeFieldMap(emp));

                byte[] docData = docRenderService.render(template.getStoredPath(), merged);
                String entryName = emp.getName() + "_任命书.docx";
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(docData);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出失败");
        }
    }

    private String encodeFilename(String filename) {
        try {
            return java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return filename;
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
