package com.xinchang.management.company.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.common.Result;
import com.xinchang.management.company.entity.Company;
import com.xinchang.management.company.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.stream.Collectors;

/**
 * 企业管理控制器，提供企业信息的增删改查、导入导出等接口
 */
@RestController
@RequestMapping("/api/companies")
@Tag(name = "企业管理", description = "企业入驻、迁出、信息维护等管理接口")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * 分页查询企业列表，支持按企业名称、房间号、楼栋、楼层筛选
     * @param companyName 企业名称（可选）
     * @param roomNumber 房间号（可选）
     * @param buildingId 楼栋ID（可选）
     * @param floorId 楼层ID（可选）
     * @param page 页码
     * @param size 每页条数
     * @return 分页企业列表
     */
    @GetMapping
    @Operation(summary = "分页查询企业列表", description = "支持按企业名称、房间号、楼栋、楼层进行筛选")
    public Result<Page<Company>> list(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(companyService.pageCompanies(companyName, roomNumber, buildingId, floorId, page, size));
    }

    /**
     * 获取企业详情，包含企业基本信息及分配的房间列表
     * @param id 企业ID
     * @return 企业详情及房间信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取企业详情", description = "根据企业ID查询企业基本信息及分配的房间")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(companyService.getCompanyDetail(id));
    }

    /**
     * 新增企业，支持绑定房间，可选上传Excel批量导入
     * @param body 企业信息（含企业名称、租约日期、房间ID列表等）
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PostMapping
    @Operation(summary = "新增企业", description = "创建新企业信息，可同时分配多个房间")
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
        List<Integer> rawCreateIds = (List<Integer>) body.get("roomIds");
        List<Long> roomIds = rawCreateIds != null ? rawCreateIds.stream().map(Integer::longValue).collect(Collectors.toList()) : null;
        companyService.createCompany(company, roomIds);
        return Result.ok();
    }

    /**
     * 更新企业信息，仅正常运营状态的企业可修改
     * @param id 企业ID
     * @param body 企业更新信息
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/{id}")
    @Operation(summary = "更新企业信息", description = "修改企业基本信息，仅NORMAL状态企业可编辑")
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
        List<Integer> rawUpdateIds = (List<Integer>) body.get("roomIds");
        List<Long> roomIds = rawUpdateIds != null ? rawUpdateIds.stream().map(Integer::longValue).collect(Collectors.toList()) : null;
        companyService.updateCompany(id, company, roomIds);
        return Result.ok();
    }

    /**
     * 变更企业业务状态（迁出/停办），迁出时自动释放房间
     * @param id 企业ID
     * @param body 包含新状态和备注信息
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/{id}/status")
    @Operation(summary = "变更企业状态", description = "变更企业运营状态，迁出时自动释放关联房间并将人员设为INACTIVE")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        companyService.changeStatus(id, body.get("businessStatus"), body.get("remark"));
        return Result.ok();
    }

    /**
     * 导出企业列表为Excel文件
     * @param companyName 企业名称筛选（可选）
     * @param roomNumber 房间号筛选（可选）
     * @param buildingId 楼栋ID筛选（可选）
     * @param floorId 楼层ID筛选（可选）
     * @return Excel文件字节数组
     */
    @GetMapping("/export")
    @Operation(summary = "导出企业列表", description = "将企业数据导出为Excel文件")
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

    /**
     * 批量导入企业信息，通过Excel文件上传
     * @param file Excel文件（.xlsx 或 .xls 格式）
     * @return 导入结果（含成功条数和错误列表）
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/import")
    @Operation(summary = "批量导入企业", description = "通过上传Excel文件批量导入企业信息")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BusinessException("上传文件为空");
        return Result.ok(companyService.importCompanies(file));
    }

    /**
     * 下载企业导入模板
     * @return Excel模板文件字节数组
     */
    @GetMapping("/template")
    @Operation(summary = "下载导入模板", description = "获取企业信息导入的标准Excel模板")
    public ResponseEntity<byte[]> template() throws IOException {
        byte[] data = companyService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import-template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
