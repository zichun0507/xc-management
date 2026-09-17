package com.xinchang.management.employee.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.xinchang.management.common.Result;
import com.xinchang.management.employee.entity.Attachment;
import com.xinchang.management.employee.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public Result<List<Attachment>> list(@RequestParam Long employeeId) {
        return Result.ok(employeeService.listAttachments(employeeId));
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public Result<Attachment> upload(
            @RequestParam Long employeeId,
            @RequestParam String attachmentType,
            @RequestParam("file") MultipartFile file) {
        return Result.ok(employeeService.uploadAttachment(employeeId, attachmentType, file));
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteAttachment(id);
        return Result.ok();
    }
}