package com.xinchang.management.room.service;

import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.room.entity.Building;
import com.xinchang.management.room.entity.Floor;
import com.xinchang.management.room.entity.Room;
import com.xinchang.management.room.mapper.BuildingMapper;
import com.xinchang.management.room.mapper.FloorMapper;
import com.xinchang.management.room.mapper.RoomMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.dev33.satoken.stp.StpUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private BuildingMapper buildingMapper;
    @Mock private FloorMapper floorMapper;
    @Mock private RoomMapper roomMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RoomService roomService;

    private Building building;
    private Floor floor;
    private Room room;

    @BeforeEach
    void setUp() {
        building = new Building();
        building.setId(1L);
        building.setName("A栋");

        floor = new Floor();
        floor.setId(1L);
        floor.setBuildingId(1L);
        floor.setName("1F");

        room = new Room();
        room.setId(1L);
        room.setBuildingId(1L);
        room.setFloorId(1L);
        room.setRoomNumber("101");
        room.setStatus("FREE");
    }

    // ==================== Room: unique number ====================

    @Test
    void createRoom_DuplicateRoomNumber_ThrowsBusinessException() {
        when(buildingMapper.selectById(1L)).thenReturn(building);
        when(floorMapper.selectById(1L)).thenReturn(floor);
        when(roomMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roomService.createRoom(1L, 1L, "101"));
        assertEquals("该楼栋下房间号已存在，请修改", ex.getMessage());
    }

    @Test
    void updateRoom_DuplicateRoomNumber_ThrowsBusinessException() {
        room.setRoomNumber("101");
        when(roomMapper.selectById(1L)).thenReturn(room);
        when(buildingMapper.selectById(1L)).thenReturn(building);
        when(floorMapper.selectById(1L)).thenReturn(floor);
        when(roomMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roomService.updateRoom(1L, 1L, 1L, "102", "FREE"));
        assertEquals("该楼栋下房间号已存在，请修改", ex.getMessage());
    }

    @Test
    void createRoom_ValidData_Success() {
        when(buildingMapper.selectById(1L)).thenReturn(building);
        when(floorMapper.selectById(1L)).thenReturn(floor);
        when(roomMapper.selectCount(any())).thenReturn(0L);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            roomService.createRoom(1L, 1L, "101");
        }

        verify(roomMapper, times(1)).insert(any(Room.class));
    }

    // ==================== Room: delete occupied ====================

    @Test
    void deleteRoom_OccupiedRoom_ThrowsBusinessException() {
        room.setStatus("OCCUPIED");
        when(roomMapper.selectById(1L)).thenReturn(room);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roomService.deleteRoom(1L));
        assertEquals("房间已分配企业，不可删除，请先解除分配", ex.getMessage());
    }

    @Test
    void deleteRoom_FreeRoom_Success() {
        room.setStatus("FREE");
        when(roomMapper.selectById(1L)).thenReturn(room);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            roomService.deleteRoom(1L);
        }

        verify(roomMapper, times(1)).deleteById(1L);
    }

    // ==================== Building: delete with floors ====================

    @Test
    void deleteBuilding_HasFloors_ThrowsBusinessException() {
        when(floorMapper.selectCount(any())).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roomService.deleteBuilding(1L));
        assertEquals("该楼栋下存在楼层数据，不可删除", ex.getMessage());
    }

    @Test
    void deleteBuilding_NoFloors_Success() {
        when(floorMapper.selectCount(any())).thenReturn(0L);
        when(buildingMapper.selectById(1L)).thenReturn(building);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            roomService.deleteBuilding(1L);
        }

        verify(buildingMapper, times(1)).deleteById(1L);
    }

    // ==================== Floor: delete with rooms ====================

    @Test
    void deleteFloor_HasRooms_ThrowsBusinessException() {
        when(roomMapper.selectCount(any())).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roomService.deleteFloor(1L));
        assertEquals("该楼层下存在房间数据，不可删除", ex.getMessage());
    }

    @Test
    void deleteFloor_NoRooms_Success() {
        when(roomMapper.selectCount(any())).thenReturn(0L);
        Floor f = new Floor();
        f.setId(1L);
        f.setName("1F");
        when(floorMapper.selectById(1L)).thenReturn(f);

        try (MockedStatic<StpUtil> stp = Mockito.mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            roomService.deleteFloor(1L);
        }

        verify(floorMapper, times(1)).deleteById(1L);
    }

    // ==================== Validation: building/floor mismatch ====================

    @Test
    void createRoom_BuildingFloorMismatch_ThrowsBusinessException() {
        Floor otherFloor = new Floor();
        otherFloor.setId(2L);
        otherFloor.setBuildingId(2L);

        when(buildingMapper.selectById(1L)).thenReturn(building);
        when(floorMapper.selectById(2L)).thenReturn(otherFloor);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> roomService.createRoom(1L, 2L, "101"));
        assertEquals("楼层不属于指定楼栋", ex.getMessage());
    }
}