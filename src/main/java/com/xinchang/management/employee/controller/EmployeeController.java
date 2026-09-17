package com.xinchang.management.employee.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.Result;
import com.xinchang.management.employee.entity.Employee;
import com.xinchang.management.employee.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public Result<Page<Employee>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(employeeService.pageEmployees(companyId, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<Employee> get(@PathVariable Long id) {
        return Result.ok(employeeService.getEmployee(id));
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public Result<Void> create(@RequestBody Employee employee) {
        employeeService.createEmployee(employee);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Employee input) {
        employeeService.updateEmployee(id, input);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        employeeService.changeStatus(id, body.get("status"));
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/{id}/photo")
    public Result<String> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return Result.ok(employeeService.uploadPhoto(id, file));
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/{id}/photo")
    public Result<Void> deletePhoto(@PathVariable Long id) {
        employeeService.deletePhoto(id);
        return Result.ok();
    }

    @GetMapping("/roster/excel")
    public ResponseEntity<byte[]> rosterExcel(@RequestParam Long companyId) throws IOException {
        byte[] data = employeeService.exportRosterExcel(companyId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=roster.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/roster/word")
    public ResponseEntity<byte[]> rosterWord(@RequestParam Long companyId) throws IOException {
        byte[] data = employeeService.exportRosterWord(companyId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=roster.docx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}