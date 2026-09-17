package com.xinchang.management.company.service;

import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.company.entity.Company;
import com.xinchang.management.company.entity.CompanyRoom;
import com.xinchang.management.company.mapper.CompanyMapper;
import com.xinchang.management.company.mapper.CompanyRoomMapper;
import com.xinchang.management.room.entity.Room;
import com.xinchang.management.room.mapper.RoomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.dev33.satoken.stp.StpUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock private CompanyMapper companyMapper;
    @Mock private CompanyRoomMapper companyRoomMapper;
    @Mock private RoomMapper roomMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CompanyService companyService;

    @Captor
    private ArgumentCaptor<Room> roomCaptor;

    private Company company;
    private Room room;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setCompanyName("测试企业");
        company.setBusinessStatus("NORMAL");

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setBuildingId(1L);
        room.setStatus("FREE");
    }

    // ==================== Room binding ====================

    @Test
    void createCompany_BindRoom_RoomStatusChanged() {
        when(companyMapper.selectCount(any())).thenReturn(0L);
        when(companyMapper.insert(any(Company.class))).thenAnswer(i -> {
            Company c = i.getArgument(0);
            c.setId(1L);
            return 1;
        });
        when(roomMapper.selectById(10L)).thenReturn(room);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            companyService.createCompany(company, List.of(10L));
        }

        verify(roomMapper, times(1)).updateById(roomCaptor.capture());
        assertEquals("OCCUPIED", roomCaptor.getValue().getStatus());
    }

    @Test
    void bindRoom_OccupiedRoom_ThrowsBusinessException() {
        room.setStatus("OCCUPIED");
        when(companyMapper.insert(any(Company.class))).thenAnswer(i -> {
            Company c = i.getArgument(0);
            c.setId(1L);
            return 1;
        });
        when(roomMapper.selectById(10L)).thenReturn(room);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> companyService.createCompany(company, List.of(10L)));
            assertTrue(ex.getMessage().contains("已被占用"));
        }
    }

    // ==================== Move out ====================

    @Test
    void changeStatus_MovedOut_ReleasesRooms() {
        company.setBusinessStatus("NORMAL");
        when(companyMapper.selectById(1L)).thenReturn(company);

        CompanyRoom cr = new CompanyRoom();
        cr.setCompanyId(1L);
        cr.setRoomId(10L);
        when(companyRoomMapper.selectList(any())).thenReturn(List.of(cr));
        when(roomMapper.selectById(10L)).thenReturn(room);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            companyService.changeStatus(1L, "MOVED_OUT", "企业迁出");
        }

        verify(companyMapper, times(1)).updateById(any());
        verify(roomMapper, times(1)).updateById(roomCaptor.capture());
        assertEquals("FREE", roomCaptor.getValue().getStatus());
        verify(jdbcTemplate, times(1)).update(
                contains("UPDATE employee"),
                eq(1L));
    }

    @Test
    void changeStatus_MovedOut_StatusUpdated() {
        when(companyMapper.selectById(1L)).thenReturn(company);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            companyService.changeStatus(1L, "MOVED_OUT", "迁出");
        }

        assertEquals("MOVED_OUT", company.getBusinessStatus());
    }

    // ==================== Status machine ====================

    @Test
    void changeStatus_InvalidStatus_ThrowsBusinessException() {
        when(companyMapper.selectById(1L)).thenReturn(company);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> companyService.changeStatus(1L, "INVALID", ""));
        assertEquals("无效的业务状态", ex.getMessage());
    }

    // ==================== Edit protection ====================

    @Test
    void updateCompany_MovedOut_ThrowsBusinessException() {
        Company movedOut = new Company();
        movedOut.setId(1L);
        movedOut.setBusinessStatus("MOVED_OUT");
        when(companyMapper.selectById(1L)).thenReturn(movedOut);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> companyService.updateCompany(1L, new Company(), null));
        assertTrue(ex.getMessage().contains("不支持编辑"));
    }
}