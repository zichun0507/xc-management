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

    public Page<Company> pageCompanies(String companyName, String roomNumber,
                                       Long buildingId, Long floorId, int pageNum, int pageSize) {
        Page<Company> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<Company>()
                .like(StringUtils.isNotBlank(companyName), Company::getCompanyName, companyName)
                .orderByDesc(Company::getCreatedTime);

        List<Long> filteredIds = getCompanyIdsByRoomFilter(roomNumber, buildingId, floorId);
        if (filteredIds != null) {
            if (filteredIds.isEmpty()) {
                return page;
            }
            wrapper.in(Company::getId, filteredIds);
        }
        return companyMapper.selectPage(page, wrapper);
    }

    public Map<String, Object> getCompanyDetail(Long id) {
        Company company = companyMapper.selectById(id);
        if (company == null) throw new BusinessException("企业不存在");

        List<CompanyRoom> assignments = companyRoomMapper.selectList(
                new LambdaQueryWrapper<CompanyRoom>().eq(CompanyRoom::getCompanyId, id));
        List<Long> roomIds = assignments.stream().map(CompanyRoom::getRoomId).collect(Collectors.toList());

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

    private List<Long> getCompanyIdsByRoomFilter(String roomNumber, Long buildingId, Long floorId) {
        if (StringUtils.isBlank(roomNumber) && buildingId == null && floorId == null) {
            return null;
        }
        LambdaQueryWrapper<CompanyRoom> crWrapper = new LambdaQueryWrapper<>();
        if (roomNumber != null || buildingId != null || floorId != null) {
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

    @Transactional(rollbackFor = Exception.class)
    public void createCompany(Company company, List<Long> roomIds) {
        if (StringUtils.isBlank(company.getCompanyName())) {
            throw new BusinessException("企业名称不能为空");
        }
        company.setBusinessStatus("NORMAL");
        companyMapper.insert(company);

        if (roomIds != null && !roomIds.isEmpty()) {
            bindRooms(company.getId(), roomIds);
        }
        writeLog("COMPANY", "CREATE", "新增企业：" + company.getCompanyName());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCompany(Long id, Company input, List<Long> roomIds) {
        Company company = companyMapper.selectById(id);
        if (company == null) throw new BusinessException("企业不存在");
        if (!"NORMAL".equals(company.getBusinessStatus())) {
            throw new BusinessException("当前企业状态不支持编辑，仅正常运营企业可修改");
        }

        if (StringUtils.isNotBlank(input.getCompanyName())) company.setCompanyName(input.getCompanyName());
        if (StringUtils.isNotBlank(input.getUnifiedCode())) company.setUnifiedCode(input.getUnifiedCode());
        if (StringUtils.isNotBlank(input.getLegalPerson())) company.setLegalPerson(input.getLegalPerson());
        if (StringUtils.isNotBlank(input.getContactPerson())) company.setContactPerson(input.getContactPerson());
        if (StringUtils.isNotBlank(input.getContactPhone())) company.setContactPhone(input.getContactPhone());
        if (StringUtils.isNotBlank(input.getAddress())) company.setAddress(input.getAddress());
        if (input.getRemark() != null) company.setRemark(input.getRemark());
        companyMapper.updateById(company);

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

        if ("MOVED_OUT".equals(newStatus) || "SUSPENDED".equals(newStatus)) {
            if ("MOVED_OUT".equals(newStatus)) {
                List<Long> roomIds = companyRoomMapper.selectList(
                                new LambdaQueryWrapper<CompanyRoom>().eq(CompanyRoom::getCompanyId, id))
                        .stream().map(CompanyRoom::getRoomId).collect(Collectors.toList());
                releaseRooms(roomIds);
            }
            jdbcTemplate.update("UPDATE employee SET status = 'INACTIVE' WHERE company_id = ? AND status = 'ACTIVE'", id);
        }

        writeLog("COMPANY", "STATUS_CHANGE",
                "企业状态变更：" + oldStatus + " → " + newStatus + "，备注：" + (remark != null ? remark : ""));
    }

    // ==================== Room Binding ====================

    private void bindRooms(Long companyId, List<Long> roomIds) {
        for (Long roomId : roomIds) {
            Room room = roomMapper.selectById(roomId);
            if (room == null) throw new BusinessException("房间ID " + roomId + " 不存在");
            if (!"FREE".equals(room.getStatus())) {
                throw new BusinessException("房间 " + room.getRoomNumber() + " 已被占用");
            }
            CompanyRoom cr = new CompanyRoom();
            cr.setCompanyId(companyId);
            cr.setRoomId(roomId);
            cr.setAllocatedTime(LocalDateTime.now());
            companyRoomMapper.insert(cr);

            room.setStatus("OCCUPIED");
            roomMapper.updateById(room);
        }
    }

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

    public byte[] downloadTemplate() throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
            SXSSFSheet sheet = wb.createSheet("导入模板");

            // ========== 新增：在创建任何行之前，开启列跟踪 ==========
            // 方式1：跟踪全部列
            sheet.trackAllColumnsForAutoSizing();
            // 方式2：只跟踪需要自适应的列（推荐，节省内存）
            // for(int i=0;i<headers.length;i++){
            //     sheet.trackColumnForAutoSizing(i);
            // }

            String[] headers = {"企业名称", "简称", "英文缩写", "统一社会信用代码", "法定代表人",
                    "联系人", "联系电话", "经营地址", "入驻开始时间", "入驻到期时间", "备注"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            sheet.createRow(1).createCell(0).setCellValue("示例企业");

            // 填充完数据后再执行自适应列宽
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