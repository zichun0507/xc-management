package com.xinchang.management.employee.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.Result;
import com.xinchang.management.employee.entity.Employee;
import com.xinchang.management.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 企业人员管理控制器，提供人员信息的增删改查、附件管理及花名册导出接口
 */
@RestController
@RequestMapping("/api/employees")
@Tag(name = "人员管理", description = "企业人员信息维护、附件上传、花名册导出等管理接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 分页查询人员列表，支持按企业ID和状态筛选
     * @param companyId 企业ID（可选）
     * @param status 人员状态（可选）
     * @param page 页码
     * @param size 每页条数
     * @return 分页人员列表
     */
    @GetMapping
    @Operation(summary = "分页查询人员列表", description = "支持按企业ID和人员状态进行筛选")
    public Result<Page<Employee>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(employeeService.pageEmployees(companyId, status, page, size));
    }

    /**
     * 获取人员详情
     * @param id 人员ID
     * @return 人员信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取人员详情", description = "根据人员ID查询详细信息")
    public Result<Employee> get(@PathVariable Long id) {
        return Result.ok(employeeService.getEmployee(id));
    }

    /**
     * 新增人员信息
     * @param employee 人员信息对象
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PostMapping
    @Operation(summary = "新增人员", description = "添加新人员信息，必须指定所属企业")
    public Result<Void> create(@RequestBody Employee employee) {
        employeeService.createEmployee(employee);
        return Result.ok();
    }

    /**
     * 更新人员信息
     * @param id 人员ID
     * @param input 更新后的人员信息
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/{id}")
    @Operation(summary = "更新人员信息", description = "修改指定人员的基本信息")
    public Result<Void> update(@PathVariable Long id, @RequestBody Employee input) {
        employeeService.updateEmployee(id, input);
        return Result.ok();
    }

    /**
     * 变更人员状态（ACTIVE/INACTIVE）
     * @param id 人员ID
     * @param body 包含新状态信息
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/status")
    @Operation(summary = "变更人员状态", description = "变更人员的在职/停办状态")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        employeeService.changeStatus(id, body.get("status"));
        return Result.ok();
    }

    /**
     * 删除人员信息
     * @param id 人员ID
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除人员", description = "删除指定人员信息")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return Result.ok();
    }

    /**
     * 上传人员照片
     * @param id 人员ID
     * @param file 照片文件（jpg/jpeg/png格式）
     * @return 照片存储路径
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/{id}/photo")
    @Operation(summary = "上传人员照片", description = "为指定人员上传证件照片")
    public Result<String> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.ok(employeeService.uploadPhoto(id, file));
    }

    /**
     * 删除人员照片
     * @param id 人员ID
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}/photo")
    @Operation(summary = "删除人员照片", description = "清除指定人员的照片")
    public Result<Void> deletePhoto(@PathVariable Long id) {
        employeeService.deletePhoto(id);
        return Result.ok();
    }

    /**
     * 导出入驻企业花名册为Excel文件
     * @param companyId 企业ID
     * @return Excel文件字节数组
     */
    @GetMapping("/roster/excel")
    @Operation(summary = "导出入驻企业花名册（Excel）", description = "将指定企业的在职人员导出为Excel花名册")
    public ResponseEntity<byte[]> rosterExcel(@RequestParam Long companyId) throws IOException {
        byte[] data = employeeService.exportRosterExcel(companyId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=roster.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /**
     * 导出入驻企业花名册为Word文档
     * @param companyId 企业ID
     * @return Word文档字节数组
     */
    @GetMapping("/roster/word")
    @Operation(summary = "导出入驻企业花名册（Word）", description = "将指定企业的在职人员导出为Word格式花名册")
    public ResponseEntity<byte[]> rosterWord(@RequestParam Long companyId) throws IOException {
        byte[] data = employeeService.exportRosterWord(companyId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=roster.docx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
