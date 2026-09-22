package com.xinchang.management.company.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.company.entity.Company;
import com.xinchang.management.company.entity.CompanyRoom;
import com.xinchang.management.company.mapper.CompanyMapper;
import com.xinchang.management.company.mapper.CompanyRoomMapper;
import com.xinchang.management.room.entity.Room;
import com.xinchang.management.room.mapper.RoomMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 企业业务逻辑服务层，提供企业信息的增删改查、状态变更、导入导出等功能
 */
@Service
public class CompanyService {

    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private CompanyRoomMapper companyRoomMapper;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<String> VALID_STATUSES = List.of("NORMAL", "MOVED_OUT", "SUSPENDED");

    // ==================== Query ====================

    /**
     * 分页查询企业列表，支持按企业名称、房间号、楼栋、楼层筛选
     * @param companyName 企业名称（模糊匹配）
     * @param roomNumber 房间号（模糊匹配）
     * @param buildingId 楼栋ID（精确匹配）
     * @param floorId 楼层ID（精确匹配）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页查询结果
     */
    public Page<Company> pageCompanies(String companyName, String roomNumber,
                                           Long buildingId, Long floorId, int pageNum, int pageSize) {
        Page<Company> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<Company>()
                .like(StringUtils.isNotBlank(companyName), Company::getCompanyName, companyName)
                .orderByDesc(Company::getCreatedTime);

        // 通过房间筛选条件获取匹配的企业ID列表
        List<Long> filteredIds = getCompanyIdsByRoomFilter(roomNumber, buildingId, floorId);
        if (filteredIds != null) {
            if (filteredIds.isEmpty()) {
                return page;
            }
            wrapper.in(Company::getId, filteredIds);
        }
        return companyMapper.selectPage(page, wrapper);
    }

    /**
     * 获取企业详情，包含企业基本信息及分配的房间列表
     * @param id 企业ID
     * @return 包含企业信息和房间列表的Map
     */
    public Map<String, Object> getCompanyDetail(Long id) {
        Company company = companyMapper.selectById(id);
        if (company == null) throw new BusinessException("企业不存在");

        // 查询该企业所有关联的房间分配记录
        List<CompanyRoom> assignments = companyRoomMapper.selectList(
                new LambdaQueryWrapper<CompanyRoom>().eq(CompanyRoom::getCompanyId, id));
        List<Long> roomIds = assignments.stream().map(CompanyRoom::getRoomId).collect(Collectors.toList());

        // 批量查询房间详情
        List<Map<String, Object>> rooms = new ArrayList<>();
        if (!roomIds.isEmpty()) {
            roomMapper.selectBatchIds(roomIds).forEach(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("roomNumber", r.getRoomNumber());
                m.put("buildingId", r.getBuildingId());
                m.put("floorId", r.getFloorId());
                rooms.add(m);
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("company", company);
        result.put("rooms", rooms);
        result.put("roomIds", roomIds);
        return result;
    }

    /**
     * 根据房间筛选条件获取匹配的企业ID列表
     * @param roomNumber 房间号
     * @param buildingId 楼栋ID
     * @param floorId 楼层ID
     * @return 匹配的企业ID列表，无筛选条件时返回null
     */
    private List<Long> getCompanyIdsByRoomFilter(String roomNumber, Long buildingId, Long floorId) {
        if (StringUtils.isBlank(roomNumber) && buildingId == null && floorId == null) {
            return null;
        }
        LambdaQueryWrapper<CompanyRoom> crWrapper = new LambdaQueryWrapper<>();
        if (roomNumber != null || buildingId != null || floorId != null) {
            // 先查询符合条件的房间，再通过房间ID关联企业
            List<Long> roomIdList = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                            .like(StringUtils.isNotBlank(roomNumber), Room::getRoomNumber, roomNumber)
                            .eq(buildingId != null, Room::getBuildingId, buildingId)
                            .eq(floorId != null, Room::getFloorId, floorId))
                    .stream().map(Room::getId).collect(Collectors.toList());
            if (roomIdList.isEmpty()) {
                return Collections.emptyList();
            }
            crWrapper.in(CompanyRoom::getRoomId, roomIdList);
        }
        List<CompanyRoom> crs = companyRoomMapper.selectList(crWrapper);
        return crs.stream().map(CompanyRoom::getCompanyId).distinct().collect(Collectors.toList());
    }

    // ==================== Create / Update ====================

    /**
     * 新增企业，校验企业名称非空后写入数据库并绑定房间
     * @param company 企业实体
     * @param roomIds 分配的房间ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void createCompany(Company company, List<Long> roomIds) {
        if (StringUtils.isBlank(company.getCompanyName())) {
            throw new BusinessException("企业名称不能为空");
        }
        company.setBusinessStatus("NORMAL");
        companyMapper.insert(company);

        // 新增成功后绑定房间
        if (roomIds != null && !roomIds.isEmpty()) {
            bindRooms(company.getId(), roomIds);
        }
        writeLog("COMPANY", "CREATE", "新增企业：" + company.getCompanyName());
    }

    /**
     * 更新企业信息，仅NORMAL状态的企业可编辑
     * @param id 企业ID
     * @param input 更新后的企业信息
     * @param roomIds 重新分配的房间ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCompany(Long id, Company input, List<Long> roomIds) {
        Company company = companyMapper.selectById(id);
        if (company == null) throw new BusinessException("企业不存在");
        // 迁出或停办状态的企业不允许编辑
        if (!"NORMAL".equals(company.getBusinessStatus())) {
            throw new BusinessException("当前企业状态不支持编辑，仅正常运营企业可修改");
        }

        // 按字段逐一更新，非空才覆盖
        if (StringUtils.isNotBlank(input.getCompanyName())) company.setCompanyName(input.getCompanyName());
        if (StringUtils.isNotBlank(input.getUnifiedCode())) company.setUnifiedCode(input.getUnifiedCode());
        if (StringUtils.isNotBlank(input.getLegalPerson())) company.setLegalPerson(input.getLegalPerson());
        if (StringUtils.isNotBlank(input.getContactPerson())) company.setContactPerson(input.getContactPerson());
        if (StringUtils.isNotBlank(input.getContactPhone())) company.setContactPhone(input.getContactPhone());
        if (StringUtils.isNotBlank(input.getAddress())) company.setAddress(input.getAddress());
        if (input.getRemark() != null) company.setRemark(input.getRemark());
        companyMapper.updateById(company);

        // 如有房间变更，先释放旧房间再绑定新房间
        if (roomIds != null) {
            List<Long> oldRoomIds = companyRoomMapper.selectList(
                            new LambdaQueryWrapper<CompanyRoom>().eq(CompanyRoom::getCompanyId, id))
                    .stream().map(CompanyRoom::getRoomId).collect(Collectors.toList());
            releaseRooms(oldRoomIds);
            companyRoomMapper.delete(new LambdaQueryWrapper<CompanyRoom>().eq(CompanyRoom::getCompanyId, id));
            bindRooms(id, roomIds);
        }
        writeLog("COMPANY", "UPDATE", "修改企业：" + company.getCompanyName());
    }

    // ==================== Status Change ====================

    /**
     * 变更企业业务状态，迁出时自动释放房间并冻结人员
     * @param id 企业ID
     * @param newStatus 新状态（NORMAL/MOVED_OUT/SUSPENDED）
     * @param remark 备注信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String newStatus, String remark) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BusinessException("无效的业务状态");
        }
        Company company = companyMapper.selectById(id);
        if (company == null) throw new BusinessException("企业不存在");

        String oldStatus = company.getBusinessStatus();
        company.setBusinessStatus(newStatus);
        company.setRemark(StringUtils.isNotBlank(remark) ? remark : company.getRemark());
        companyMapper.updateById(company);

        // 迁出或停办时，释放房间并将企业下所有人员状态设为INACTIVE
        if ("MOVED_OUT".equals(newStatus) || "SUSPENDED".equals(newStatus)) {
            if ("MOVED_OUT".equals(newStatus)) {
                // 迁出：释放所有已分配的房间
                List<Long> roomIds = companyRoomMapper.selectList(
                                new LambdaQueryWrapper<CompanyRoom>().eq(CompanyRoom::getCompanyId, id))
                        .stream().map(CompanyRoom::getRoomId).collect(Collectors.toList());
                releaseRooms(roomIds);
            }
            // 停办或迁出均需将人员设为INACTIVE
            jdbcTemplate.update("UPDATE employee SET status = 'INACTIVE' WHERE company_id = ? AND status = 'ACTIVE'", id);
        }

        writeLog("COMPANY", "STATUS_CHANGE",
                "企业状态变更：" + oldStatus + " → " + newStatus + "，备注：" + (remark != null ? remark : ""));
    }

    // ==================== Room Binding ====================

    /**
     * 将指定房间绑定到企业，校验房间存在且为空闲状态
     * @param companyId 企业ID
     * @param roomIds 房间ID列表
     */
    private void bindRooms(Long companyId, List<Long> roomIds) {
        for (Long roomId : roomIds) {
            Room room = roomMapper.selectById(roomId);
            if (room == null) throw new BusinessException("房间ID " + roomId + " 不存在");
            // 房间必须为空闲状态才可分配
            if (!"FREE".equals(room.getStatus())) {
                throw new BusinessException("房间 " + room.getRoomNumber() + " 已被占用");
            }
            CompanyRoom cr = new CompanyRoom();
            cr.setCompanyId(companyId);
            cr.setRoomId(roomId);
            cr.setAllocatedTime(LocalDateTime.now());
            companyRoomMapper.insert(cr);

            // 更新房间状态为占用
            room.setStatus("OCCUPIED");
            roomMapper.updateById(room);
        }
    }

    /**
     * 释放指定房间，将状态重置为FREE
     * @param roomIds 房间ID列表
     */
    private void releaseRooms(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return;
        for (Long roomId : roomIds) {
            Room room = roomMapper.selectById(roomId);
            if (room != null) {
                room.setStatus("FREE");
                roomMapper.updateById(room);
            }
        }
    }

    // ==================== Export ====================

    /**
     * 导出企业列表为Excel文件，包含企业基本信息及分配的房间号
     * @param companyName 企业名称筛选
     * @param roomNumber 房间号筛选
     * @param buildingId 楼栋ID筛选
     * @param floorId 楼层ID筛选
     * @return Excel文件的字节数组
     */
    public byte[] exportCompanies(String companyName, String roomNumber,
                                    Long buildingId, Long floorId) throws IOException {
        List<Company> companies = getFilteredList(companyName, roomNumber, buildingId, floorId);

        try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
            Sheet sheet = wb.createSheet("入驻企业");
            String[] headers = {"企业全称", "简称", "英文缩写", "统一社会信用代码", "法定代表人",
                    "联系人", "联系电话", "经营地址", "入驻开始时间", "入驻到期时间", "业务状态", "备注", "分配房间号"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Company c : companies) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getCompanyName());
                row.createCell(1).setCellValue(c.getShortName());
                row.createCell(2).setCellValue(c.getEnglishName());
                row.createCell(3).setCellValue(c.getUnifiedCode());
                row.createCell(4).setCellValue(c.getLegalPerson());
                row.createCell(5).setCellValue(c.getContactPerson());
                row.createCell(6).setCellValue(c.getContactPhone());
                row.createCell(7).setCellValue(c.getAddress());
                row.createCell(8).setCellValue(c.getLeaseStartDate() != null ? c.getLeaseStartDate().toString() : "");
                row.createCell(9).setCellValue(c.getLeaseEndDate() != null ? c.getLeaseEndDate().toString() : "");
                row.createCell(10).setCellValue(statusLabel(c.getBusinessStatus()));
                row.createCell(11).setCellValue(c.getRemark());

                // 查询并拼接该企业分配的所有房间号
                List<Long> roomIds = companyRoomMapper.selectList(
                                new LambdaQueryWrapper<CompanyRoom>().eq(CompanyRoom::getCompanyId, c.getId()))
                        .stream().map(CompanyRoom::getRoomId).collect(Collectors.toList());
                String roomNumbers = "";
                if (!roomIds.isEmpty()) {
                    roomNumbers = roomMapper.selectBatchIds(roomIds).stream()
                            .map(Room::getRoomNumber).collect(Collectors.joining(", "));
                }
                row.createCell(12).setCellValue(roomNumbers);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ==================== Import ====================

    /**
     * 通过Excel文件批量导入企业信息，逐行解析并写入数据库
     * @param file Excel文件（.xlsx 或 .xls 格式）
     * @return 导入结果，包含成功条数和错误列表
     */
    public Map<String, Object> importCompanies(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new BusinessException("请上传 .xlsx 或 .xls 格式的 Excel 文件");
        }

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int totalRows = sheet.getPhysicalNumberOfRows() - 1;
            if (totalRows <= 0) {
                throw new BusinessException("Excel 文件无数据行");
            }

            int successCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            // 从第二行开始逐行解析数据
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                int rowNum = i + 1;
                try {
                    String name = getCellString(row.getCell(0));
                    if (StringUtils.isBlank(name)) {
                        throw new BusinessException("企业名称不能为空");
                    }
                    Company company = new Company();
                    company.setCompanyName(name);
                    company.setShortName(getCellString(row.getCell(1)));
                    company.setEnglishName(getCellString(row.getCell(2)));
                    company.setUnifiedCode(getCellString(row.getCell(3)));
                    company.setLegalPerson(getCellString(row.getCell(4)));
                    company.setContactPerson(getCellString(row.getCell(5)));
                    company.setContactPhone(getCellString(row.getCell(6)));
                    company.setAddress(getCellString(row.getCell(7)));
                    String startDate = getCellString(row.getCell(8));
                    if (StringUtils.isNotBlank(startDate))
                        company.setLeaseStartDate(java.time.LocalDate.parse(startDate));
                    String endDate = getCellString(row.getCell(9));
                    if (StringUtils.isNotBlank(endDate)) company.setLeaseEndDate(java.time.LocalDate.parse(endDate));
                    company.setRemark(getCellString(row.getCell(10)));
                    company.setBusinessStatus("NORMAL");
                    companyMapper.insert(company);
                    successCount++;
                } catch (Exception e) {
                    // 记录单行错误但继续导入其余行
                    Map<String, Object> err = new HashMap<>();
                    err.put("row", rowNum);
                    err.put("reason", e instanceof BusinessException ? e.getMessage() : "数据格式错误");
                    errors.add(err);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("totalCount", totalRows);
            result.put("errors", errors);
            writeLog("COMPANY", "IMPORT", "批量导入企业：" + successCount + "/" + totalRows + " 条成功");
            return result;
        }
    }

    // ==================== Template ====================

    /**
     * 生成企业导入模板Excel文件
     * @return 模板Excel文件的字节数组
     */
    public byte[] downloadTemplate() throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
            SXSSFSheet sheet = wb.createSheet("导入模板");
            sheet.trackAllColumnsForAutoSizing();

            String[] headers = {"企业名称", "简称", "英文缩写", "统一社会信用代码", "法定代表人",
                    "联系人", "联系电话", "经营地址", "入驻开始时间", "入驻到期时间", "备注"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            sheet.createRow(1).createCell(0).setCellValue("示例企业");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ==================== Internal ====================

    private List<Company> getFilteredList(String companyName, String roomNumber,
                                          Long buildingId, Long floorId) {
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<Company>()
                .like(StringUtils.isNotBlank(companyName), Company::getCompanyName, companyName)
                .orderByDesc(Company::getCreatedTime);
        List<Long> filteredIds = getCompanyIdsByRoomFilter(roomNumber, buildingId, floorId);
        if (filteredIds != null) {
            if (filteredIds.isEmpty()) return Collections.emptyList();
            wrapper.in(Company::getId, filteredIds);
        }
        return companyMapper.selectList(wrapper);
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "NORMAL" -> "正常运营";
            case "MOVED_OUT" -> "迁出";
            case "SUSPENDED" -> "停办";
            default -> status;
        };
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
