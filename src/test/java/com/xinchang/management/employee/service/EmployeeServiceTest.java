package com.xinchang.management.employee.service;

import cn.dev33.satoken.stp.StpUtil;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.employee.entity.Attachment;
import com.xinchang.management.employee.entity.Employee;
import com.xinchang.management.employee.mapper.AttachmentMapper;
import com.xinchang.management.employee.mapper.EmployeeMapper;
import com.xinchang.management.company.mapper.CompanyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeMapper employeeMapper;
    @Mock private AttachmentMapper attachmentMapper;
    @Mock private CompanyMapper companyMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private EmployeeService employeeService;

    @TempDir
    Path tempDir;

    private Employee employee;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(employeeService, "uploadBasePath", tempDir.toString());

        employee = new Employee();
        employee.setId(1L);
        employee.setCompanyId(1L);
        employee.setName("张三");
        employee.setStatus("ACTIVE");
    }

    // ==================== Status change log ====================

    @Test
    void changeStatus_ActiveToInactive_LogsWithOldAndNewStatus() {
        when(employeeMapper.selectById(1L)).thenReturn(employee);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            employeeService.changeStatus(1L, "INACTIVE");
        }

        verify(employeeMapper, times(1)).updateById(argThat(e ->
                "INACTIVE".equals(e.getStatus())));
        verify(jdbcTemplate, times(1)).update(
                contains("operation_log"), any(), any(), eq("EMPLOYEE"),
                eq("STATUS_CHANGE"), contains("ACTIVE → INACTIVE"));
    }

    @Test
    void changeStatus_InactiveToActive_LogsCorrectly() {
        employee.setStatus("INACTIVE");
        when(employeeMapper.selectById(1L)).thenReturn(employee);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            employeeService.changeStatus(1L, "ACTIVE");
        }

        verify(jdbcTemplate, times(1)).update(
                contains("operation_log"), any(), any(), eq("EMPLOYEE"),
                eq("STATUS_CHANGE"), contains("INACTIVE → ACTIVE"));
    }

    @Test
    void changeStatus_InvalidStatus_ThrowsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> employeeService.changeStatus(1L, "INVALID"));
        assertEquals("无效的人员状态", ex.getMessage());
    }

    @Test
    void changeStatus_SameStatus_DoesNothing() {
        when(employeeMapper.selectById(1L)).thenReturn(employee);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            employeeService.changeStatus(1L, "ACTIVE");
        }

        verify(employeeMapper, never()).updateById(any());
        verify(jdbcTemplate, never()).update(contains("operation_log"), any(), any());
    }

    // ==================== File validation ====================

    @Test
    void uploadAttachment_ExecutableExtension_Rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "bad".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> employeeService.uploadAttachment(1L, "ID_CARD_FRONT", file));
        assertTrue(ex.getMessage().contains("禁止上传可执行文件"));
    }

    @Test
    void uploadAttachment_WrongImageFormat_Rejected() {
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.gif", "image/gif", "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> employeeService.uploadAttachment(1L, "ID_CARD_FRONT", file));
        assertTrue(ex.getMessage().contains("不支持的文件格式"));
    }

    @Test
    void uploadAttachment_EducationReportRequiresPdf_Rejected() {
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.png", "image/png", "data".getBytes());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> employeeService.uploadAttachment(1L, "EDUCATION_REPORT", file));
        assertTrue(ex.getMessage().contains("不支持的文件格式"));
    }

    @Test
    void uploadAttachment_TooLarge_Rejected() {
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        byte[] big = new byte[60 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", big);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> employeeService.uploadAttachment(1L, "ID_CARD_FRONT", file));
        assertTrue(ex.getMessage().contains("文件大小超过限制"));
    }

    @Test
    void uploadAttachment_ValidFormat_Success() {
        when(employeeMapper.selectById(1L)).thenReturn(employee);
        MockMultipartFile file = new MockMultipartFile(
                "file", "idcard.jpg", "image/jpeg", "photo-data".getBytes());

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            Attachment result = employeeService.uploadAttachment(1L, "ID_CARD_FRONT", file);
            assertNotNull(result);
            assertEquals("ID_CARD_FRONT", result.getAttachmentType());
        }
    }

    // ==================== Employee creation ====================

    @Test
    void createEmployee_NoCompany_ThrowsBusinessException() {
        Employee e = new Employee();
        e.setName("李四");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> employeeService.createEmployee(e));
        assertTrue(ex.getMessage().contains("必须指定所属企业"));
    }

    @Test
    void createEmployee_DefaultStatusIsActive() {
        Employee e = new Employee();
        e.setName("李四");
        e.setCompanyId(1L);
        when(companyMapper.selectById(1L)).thenReturn(new com.xinchang.management.company.entity.Company());
        when(employeeMapper.insert(any())).thenReturn(1);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            employeeService.createEmployee(e);
        }

        assertEquals("ACTIVE", e.getStatus());
    }
}