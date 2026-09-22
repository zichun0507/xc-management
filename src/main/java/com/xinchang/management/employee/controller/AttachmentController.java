package com.xinchang.management.employee.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xinchang.management.common.Result;
import com.xinchang.management.employee.entity.Attachment;
import com.xinchang.management.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 人员附件管理控制器，提供身份证、学历等附件的上传与删除接口
 */
@RestController
@RequestMapping("/api/attachments")
@Tag(name = "附件管理", description = "人员身份证、学历报告等附件的上传与管理接口")
public class AttachmentController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 查询指定人员的所有附件列表
     * @param employeeId 人员ID
     * @return 附件列表
     */
    @GetMapping
    @Operation(summary = "查询附件列表", description = "获取指定人员的所有附件信息")
    public Result<List<Attachment>> list(@RequestParam Long employeeId) {
        return Result.ok(employeeService.listAttachments(employeeId));
    }

    /**
     * 上传人员附件（身份证正面、身份证背面、学历报告等）
     * @param employeeId 人员ID
     * @param attachmentType 附件类型
     * @param file 附件文件
     * @return 附件信息
     */
    @SaCheckRole("ADMIN")
    @PostMapping
    @Operation(summary = "上传附件", description = "为指定人员上传身份证、学历报告等附件")
    public Result<Attachment> upload(
            @RequestParam Long employeeId,
            @RequestParam String attachmentType,
            @RequestParam("file") MultipartFile file) {
        return Result.ok(employeeService.uploadAttachment(employeeId, attachmentType, file));
    }

    /**
     * 删除指定附件
     * @param id 附件ID
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除附件", description = "根据附件ID删除对应附件记录")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteAttachment(id);
        return Result.ok();
    }
}
