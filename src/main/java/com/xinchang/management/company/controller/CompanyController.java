package com.xinchang.management.company.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.common.Result;
import com.xinchang.management.company.entity.Company;
import com.xinchang.management.company.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public Result<Page<Company>> list(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(companyService.pageCompanies(companyName, roomNumber, buildingId, floorId, page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(companyService.getCompanyDetail(id));
    }

    @SaCheckRole("ADMIN")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Company company = new Company();
        company.setCompanyName((String) body.get("companyName"));
        company.setShortName((String) body.get("shortName"));
        company.setEnglishName((String) body.get("englishName"));
        company.setUnifiedCode((String) body.get("unifiedCode"));
        company.setLegalPerson((String) body.get("legalPerson"));
        company.setContactPerson((String) body.get("contactPerson"));
        company.setContactPhone((String) body.get("contactPhone"));
        company.setAddress((String) body.get("address"));
        company.setRemark((String) body.get("remark"));
        String start = (String) body.get("leaseStartDate");
        if (start != null && !start.isBlank()) company.setLeaseStartDate(LocalDate.parse(start));
        String end = (String) body.get("leaseEndDate");
        if (end != null && !end.isBlank()) company.setLeaseEndDate(LocalDate.parse(end));
        @SuppressWarnings("unchecked")
        List<Integer> rawCreateIds = (List<Integer>) body.get("roomIds");
        List<Long> roomIds = rawCreateIds != null ? rawCreateIds.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList()) : null;
        companyService.createCompany(company, roomIds);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Company company = new Company();
        company.setCompanyName((String) body.get("companyName"));
        company.setShortName((String) body.get("shortName"));
        company.setEnglishName((String) body.get("englishName"));
        company.setUnifiedCode((String) body.get("unifiedCode"));
        company.setLegalPerson((String) body.get("legalPerson"));
        company.setContactPerson((String) body.get("contactPerson"));
        company.setContactPhone((String) body.get("contactPhone"));
        company.setAddress((String) body.get("address"));
        company.setRemark((String) body.get("remark"));
        String startUpd = (String) body.get("leaseStartDate");
        if (startUpd != null && !startUpd.isBlank()) company.setLeaseStartDate(LocalDate.parse(startUpd));
        String endUpd = (String) body.get("leaseEndDate");
        if (endUpd != null && !endUpd.isBlank()) company.setLeaseEndDate(LocalDate.parse(endUpd));
        @SuppressWarnings("unchecked")
        List<Integer> rawUpdateIds = (List<Integer>) body.get("roomIds");
        List<Long> roomIds = rawUpdateIds != null ? rawUpdateIds.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList()) : null;
        companyService.updateCompany(id, company, roomIds);
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        companyService.changeStatus(id, body.get("businessStatus"), body.get("remark"));
        return Result.ok();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId) throws IOException {
        byte[] data = companyService.exportCompanies(companyName, roomNumber, buildingId, floorId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=companies.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BusinessException("上传文件为空");
        return Result.ok(companyService.importCompanies(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] data = companyService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import-template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}