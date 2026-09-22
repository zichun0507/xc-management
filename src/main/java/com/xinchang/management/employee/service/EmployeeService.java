package com.xinchang.management.employee.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.company.mapper.CompanyMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.employee.entity.Attachment;
import com.xinchang.management.employee.entity.Employee;
import com.xinchang.management.employee.mapper.AttachmentMapper;
import com.xinchang.management.employee.mapper.EmployeeMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 企业人员业务逻辑服务层，提供人员信息的增删改查、附件管理及花名册导出功能
 */
@Service
public class EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private AttachmentMapper attachmentMapper;
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.upload.base-path:./uploads}")
    private String uploadBasePath;

    private static final Set<String> IMAGE_FORMATS = Set.of("jpg", "jpeg", "png", "bmp", "gif");
    private static final Set<String> DOC_FORMATS = Set.of("pdf");
    private static final Set<String> PHOTO_FORMATS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> EXECUTABLE_FORMATS = Set.of("exe", "bat", "sh", "dll", "com", "scr", "pif", "vbs", "msi", "reg", "cmd", "ps1");
    private static final Set<String> ATTACHMENT_TYPES = Set.of("ID_CARD_FRONT", "ID_CARD_BACK", "GRADUATION_CERT", "EDUCATION_REPORT");
    private static final Map<String, Set<String>> ATTACHMENT_ALLOWED_FORMATS = Map.of(
            "ID_CARD_FRONT", Set.of("jpg", "jpeg", "png"),
            "ID_CARD_BACK", Set.of("jpg", "jpeg", "png"),
            "GRADUATION_CERT", Set.of("jpg", "jpeg", "png"),
            "EDUCATION_REPORT", Set.of("pdf"));
    private static final long PHOTO_MAX_SIZE = 10 * 1024 * 1024;
    private static final long ATTACHMENT_MAX_SIZE = 50 * 1024 * 1024;

    // ==================== Employee CRUD ====================

    /**
     * 分页查询人员列表，支持按企业ID和状态筛选
     * @param companyId 企业ID
     * @param status 人员状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页人员列表
     */
    public Page<Employee> pageEmployees(Long companyId, String status, int pageNum, int pageSize) {
        return employeeMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Employee>()
                        .eq(companyId != null, Employee::getCompanyId, companyId)
                        .eq(StringUtils.isNotBlank(status), Employee::getStatus, status)
                        .orderByDesc(Employee::getCreatedTime));
    }

    /**
     * 获取人员详情
     * @param id 人员ID
     * @return 人员实体
     */
    public Employee getEmployee(Long id) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) throw new BusinessException("人员不存在");
        return emp;
    }

    /**
     * 新增人员信息，校验企业存在性及姓名非空
     * @param employee 人员实体
     */
    @Transactional(rollbackFor = Exception.class)
    public void createEmployee(Employee employee) {
        if (StringUtils.isBlank(employee.getName())) throw new BusinessException("姓名不能为空");
        if (employee.getCompanyId() == null) throw new BusinessException("必须指定所属企业");
        // 校验所属企业是否存在
        if (companyMapper.selectById(employee.getCompanyId()) == null) {
            throw new BusinessException("所属企业不存在");
        }
        employee.setStatus("ACTIVE");
        employeeMapper.insert(employee);
        writeLog("EMPLOYEE", "CREATE", "新增人员：" + employee.getName() + "，企业ID：" + employee.getCompanyId());
    }

    /**
     * 更新人员信息，按字段逐一更新非空值
     * @param id 人员ID
     * @param input 更新后的人员信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(Long id, Employee input) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) throw new BusinessException("人员不存在");
        // 按字段逐一更新，仅非空值覆盖
        if (StringUtils.isNotBlank(input.getName())) emp.setName(input.getName());
        if (input.getGender() != null) emp.setGender(input.getGender());
        if (input.getIdCard() != null) emp.setIdCard(input.getIdCard());
        if (input.getSchool() != null) emp.setSchool(input.getSchool());
        if (input.getMajor() != null) emp.setMajor(input.getMajor());
        if (input.getPhone() != null) emp.setPhone(input.getPhone());
        if (input.getPosition() != null) emp.setPosition(input.getPosition());
        if (input.getPhoto() != null) emp.setPhoto(input.getPhoto());
        employeeMapper.updateById(emp);
        writeLog("EMPLOYEE", "UPDATE", "修改人员信息：" + emp.getName());
    }

    /**
     * 变更人员状态，仅ACTIVE和INACTIVE两种状态合法
     * @param id 人员ID
     * @param newStatus 新状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String newStatus) {
        if (!Set.of("ACTIVE", "INACTIVE").contains(newStatus)) {
            throw new BusinessException("无效的人员状态");
        }
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) throw new BusinessException("人员不存在");
        String oldStatus = emp.getStatus();
        // 状态未变化则无需操作
        if (oldStatus.equals(newStatus)) return;
        emp.setStatus(newStatus);
        employeeMapper.updateById(emp);
        writeLog("EMPLOYEE", "STATUS_CHANGE", "人员状态变更：" + emp.getName() + "，" + oldStatus + " → " + newStatus);
    }

    /**
     * 删除人员信息
     * @param id 人员ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployee(Long id) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) throw new BusinessException("人员不存在");
        employeeMapper.deleteById(id);
        writeLog("EMPLOYEE", "DELETE", "删除人员：" + emp.getName());
    }

    // ==================== Photo ====================

    /**
     * 上传人员照片，校验图片格式及大小
     * @param employeeId 人员ID
     * @param file 照片文件
     * @return 照片存储路径
     */
    public String uploadPhoto(Long employeeId, MultipartFile file) {
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) throw new BusinessException("人员不存在");

        String ext = validateFile(file, PHOTO_FORMATS, PHOTO_MAX_SIZE);

        String dir = uploadBasePath + "/photos/";
        ensureDir(dir);
        String filename = employeeId + "_" + UUID.randomUUID() + "." + ext;
        String fullPath = dir + filename;
        saveFile(file, fullPath);

        emp.setPhoto(fullPath);
        employeeMapper.updateById(emp);
        return fullPath;
    }

    /**
     * 删除人员照片，将照片路径置空
     * @param employeeId 人员ID
     */
    public void deletePhoto(Long employeeId) {
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) throw new BusinessException("人员不存在");
        emp.setPhoto(null);
        employeeMapper.updateById(emp);
    }

    // ==================== Attachments ====================

    /**
     * 查询指定人员的所有附件
     * @param employeeId 人员ID
     * @return 附件列表
     */
    public List<Attachment> listAttachments(Long employeeId) {
        return attachmentMapper.selectList(
                new LambdaQueryWrapper<Attachment>()
                        .eq(Attachment::getEmployeeId, employeeId)
                        .orderByDesc(Attachment::getUploadedTime));
    }

    /**
     * 上传人员附件，同一类型附件先删除旧记录再插入新记录
     * @param employeeId 人员ID
     * @param attachmentType 附件类型
     * @param file 附件文件
     * @return 附件实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Attachment uploadAttachment(Long employeeId, String attachmentType, MultipartFile file) {
        if (!ATTACHMENT_TYPES.contains(attachmentType)) {
            throw new BusinessException("无效的附件类型：" + attachmentType);
        }
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) throw new BusinessException("人员不存在");

        // 校验文件格式和大小
        Set<String> allowed = ATTACHMENT_ALLOWED_FORMATS.get(attachmentType);
        String ext = validateFile(file, allowed, ATTACHMENT_MAX_SIZE);

        // 同一类型附件先删除旧记录，实现覆盖更新
        attachmentMapper.delete(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getEmployeeId, employeeId)
                .eq(Attachment::getAttachmentType, attachmentType));

        String dir = uploadBasePath + "/attachments/" + employeeId + "/";
        ensureDir(dir);
        String filename = attachmentType + "_" + UUID.randomUUID() + "." + ext;
        String fullPath = dir + filename;
        saveFile(file, fullPath);

        Attachment attachment = new Attachment();
        attachment.setEmployeeId(employeeId);
        attachment.setAttachmentType(attachmentType);
        attachment.setOriginalName(file.getOriginalFilename());
        attachment.setStoredPath(fullPath);
        attachment.setFileSize(file.getSize());
        attachment.setFileFormat(ext);
        attachment.setUploadedTime(LocalDateTime.now());
        attachmentMapper.insert(attachment);
        return attachment;
    }

    /**
     * 删除指定附件记录
     * @param id 附件ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long id) {
        Attachment att = attachmentMapper.selectById(id);
        if (att == null) throw new BusinessException("附件不存在");
        attachmentMapper.deleteById(id);
    }

    // ==================== Roster Export ====================

    /**
     * 导出入驻企业花名册为Excel文件，包含人员信息和照片
     * @param companyId 企业ID
     * @return Excel文件字节数组
     */
    public byte[] exportRosterExcel(Long companyId) throws IOException {
        List<Employee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getCompanyId, companyId)
                        .eq(Employee::getStatus, "ACTIVE")
                        .orderByAsc(Employee::getName));

        try (SXSSFWorkbook wb = new SXSSFWorkbook()) {
            Sheet sheet = wb.createSheet("花名册");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"姓名", "毕业学校", "专业", "身份证号", "照片"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            CreationHelper helper = wb.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            int rowIdx = 1;
            for (Employee emp : employees) {
                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(emp.getName());
                row.createCell(1).setCellValue(emp.getSchool());
                row.createCell(2).setCellValue(emp.getMajor());
                row.createCell(3).setCellValue(emp.getIdCard());

                // 插入照片到Excel单元格
                if (StringUtils.isNotBlank(emp.getPhoto())) {
                    try {
                        File photoFile = new File(emp.getPhoto());
                        if (photoFile.exists()) {
                            String photoExt = getExt(emp.getPhoto()).toLowerCase();
                            int picType = switch (photoExt) {
                                case "png" -> Workbook.PICTURE_TYPE_PNG;
                                case "jpg", "jpeg" -> Workbook.PICTURE_TYPE_JPEG;
                                default -> Workbook.PICTURE_TYPE_JPEG;
                            };
                            byte[] pictureData = Files.readAllBytes(photoFile.toPath());
                            int pictureIdx = wb.addPicture(pictureData, picType);
                            ClientAnchor anchor = helper.createClientAnchor();
                            anchor.setCol1(4);
                            anchor.setRow1(rowIdx);
                            anchor.setCol2(5);
                            anchor.setRow2(rowIdx + 1);
                            drawing.createPicture(anchor, pictureIdx);
                            row.setHeightInPoints(60);
                        }
                    } catch (Exception ignored) {
                    }
                }
                rowIdx++;
            }

            sheet.setColumnWidth(0, 3000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 5000);
            sheet.setColumnWidth(3, 6000);
            sheet.setColumnWidth(4, 4000);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 导出入驻企业花名册为Word文档，包含人员信息和照片
     * @param companyId 企业ID
     * @return Word文档字节数组
     */
    public byte[] exportRosterWord(Long companyId) throws IOException {
        List<Employee> employees = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>()
                        .eq(Employee::getCompanyId, companyId)
                        .eq(Employee::getStatus, "ACTIVE")
                        .orderByAsc(Employee::getName));

        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFTable table = doc.createTable(employees.size() + 1, 5);
            table.setWidth("100%");

            String[] headers = {"姓名", "毕业学校", "专业", "身份证号", "照片"};
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.getCell(i).setText(headers[i]);
            }

            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                XWPFTableRow row = table.getRow(i + 1);
                row.getCell(0).setText(emp.getName());
                row.getCell(1).setText(emp.getSchool());
                row.getCell(2).setText(emp.getMajor());
                row.getCell(3).setText(emp.getIdCard());

                // 在最后一列插入照片
                XWPFTableCell photoCell = row.getCell(4);
                if (StringUtils.isNotBlank(emp.getPhoto())) {
                    try {
                        File photoFile = new File(emp.getPhoto());
                        if (photoFile.exists()) {
                            String photoExt = getExt(emp.getPhoto()).toLowerCase();
                            int docPicType = switch (photoExt) {
                                case "png" -> XWPFDocument.PICTURE_TYPE_PNG;
                                case "jpg", "jpeg" -> XWPFDocument.PICTURE_TYPE_JPEG;
                                default -> XWPFDocument.PICTURE_TYPE_JPEG;
                            };
                            photoCell.removeParagraph(0);
                            XWPFParagraph para = photoCell.addParagraph();
                            XWPFRun run = para.createRun();
                            try (FileInputStream fis = new FileInputStream(photoFile)) {
                                run.addPicture(fis, docPicType, "photo." + photoExt, Units.toEMU(80), Units.toEMU(100));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    // ==================== File Internal ====================

    /**
     * 校验上传文件的格式和大小，禁止可执行文件
     * @param file 上传文件
     * @param allowedFormats 允许的文件格式集合
     * @param maxSize 最大文件大小（字节）
     * @return 文件扩展名
     */
    private String validateFile(MultipartFile file, Set<String> allowedFormats, long maxSize) {
        if (file.isEmpty()) throw new BusinessException("上传文件为空");
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制（最大 " + (maxSize / 1024 / 1024) + "MB）");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null) throw new BusinessException("文件名不能为空");
        String ext = getExt(originalName).toLowerCase();
        // 禁止上传可执行文件，确保安全
        if (EXECUTABLE_FORMATS.contains(ext)) {
            throw new BusinessException("禁止上传可执行文件（." + ext + "）");
        }
        if (!allowedFormats.contains(ext)) {
            throw new BusinessException("不支持的文件格式（." + ext + "），允许格式：" + String.join(", ", allowedFormats));
        }
        return ext;
    }

    private String getExt(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx == -1 ? "" : filename.substring(idx + 1);
    }

    private void ensureDir(String dir) {
        Path path = Paths.get(dir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new BusinessException("创建上传目录失败");
            }
        }
    }

    private void saveFile(MultipartFile file, String fullPath) {
        try {
            Files.copy(file.getInputStream(), Paths.get(fullPath));
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }
    }

    // ==================== Log ====================

    private void writeLog(String module, String action, String content) {
        long operatorId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(operatorId);
        String operatorName = user != null ? user.getRealName() : String.valueOf(operatorId);
        jdbcTemplate.update(
                "INSERT INTO operation_log (operator_id, operator_name, module, action, content) VALUES (?, ?, ?, ?, ?)",
                operatorId, operatorName, module, action, content);
    }
}
