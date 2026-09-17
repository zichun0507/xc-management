package com.xinchang.management.room.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.auth.entity.SysUser;
import com.xinchang.management.auth.mapper.SysUserMapper;
import com.xinchang.management.common.BusinessException;
import com.xinchang.management.room.entity.Building;
import com.xinchang.management.room.entity.Floor;
import com.xinchang.management.room.entity.Room;
import com.xinchang.management.room.mapper.BuildingMapper;
import com.xinchang.management.room.mapper.FloorMapper;
import com.xinchang.management.room.mapper.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    @Autowired
    private BuildingMapper buildingMapper;
    @Autowired
    private FloorMapper floorMapper;
    @Autowired
    private RoomMapper roomMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<String> ROOM_STATUSES = List.of("FREE", "OCCUPIED", "DISABLED");

    // ==================== Building ====================

    public List<Building> listBuildings() {
        return buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().orderByAsc(Building::getSortOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createBuilding(String name, String landlord, Integer sortOrder) {
        Building building = new Building();
        building.setName(name);
        building.setLandlord(landlord);
        building.setSortOrder(sortOrder != null ? sortOrder : 0);
        buildingMapper.insert(building);
        writeLog("BUILDING", "CREATE", "新增楼栋：" + name);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateBuilding(Long id, String name, String landlord, Integer sortOrder) {
        Building building = buildingMapper.selectById(id);
        if (building == null) throw new BusinessException("楼栋不存在");
        building.setName(name);
        if (landlord != null) building.setLandlord(landlord);
        building.setSortOrder(sortOrder != null ? sortOrder : 0);
        buildingMapper.updateById(building);
        writeLog("BUILDING", "UPDATE", "修改楼栋：" + building.getName() + " → " + name);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBuilding(Long id) {
        Long floorCount = floorMapper.selectCount(
                new LambdaQueryWrapper<Floor>().eq(Floor::getBuildingId, id));
        if (floorCount > 0) {
            throw new BusinessException("该楼栋下存在楼层数据，不可删除");
        }
        Building building = buildingMapper.selectById(id);
        if (building == null) throw new BusinessException("楼栋不存在");
        buildingMapper.deleteById(id);
        writeLog("BUILDING", "DELETE", "删除楼栋：" + building.getName());
    }

    // ==================== Floor ====================

    public List<Floor> listFloors(Long buildingId) {
        return floorMapper.selectList(
                new LambdaQueryWrapper<Floor>()
                        .eq(buildingId != null, Floor::getBuildingId, buildingId)
                        .orderByAsc(Floor::getSortOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createFloor(Long buildingId, String name, Integer sortOrder) {
        if (buildingMapper.selectById(buildingId) == null) {
            throw new BusinessException("所属楼栋不存在");
        }
        Floor floor = new Floor();
        floor.setBuildingId(buildingId);
        floor.setName(name);
        floor.setSortOrder(sortOrder != null ? sortOrder : 0);
        floorMapper.insert(floor);
        writeLog("FLOOR", "CREATE", "新增楼层：" + name);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFloor(Long id, Long buildingId, String name, Integer sortOrder) {
        Floor floor = floorMapper.selectById(id);
        if (floor == null) throw new BusinessException("楼层不存在");
        floor.setBuildingId(buildingId);
        floor.setName(name);
        floor.setSortOrder(sortOrder != null ? sortOrder : 0);
        floorMapper.updateById(floor);
        writeLog("FLOOR", "UPDATE", "修改楼层：" + floor.getName() + " → " + name);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFloor(Long id) {
        Long roomCount = roomMapper.selectCount(
                new LambdaQueryWrapper<Room>().eq(Room::getFloorId, id));
        if (roomCount > 0) {
            throw new BusinessException("该楼层下存在房间数据，不可删除");
        }
        Floor floor = floorMapper.selectById(id);
        if (floor == null) throw new BusinessException("楼层不存在");
        floorMapper.deleteById(id);
        writeLog("FLOOR", "DELETE", "删除楼层：" + floor.getName());
    }

    // ==================== Room ====================

    public Page<Room> pageRooms(Long buildingId, Long floorId, String roomNumber,
                                String status, int pageNum, int pageSize) {
        Page<Room> page = new Page<>(pageNum, pageSize);
        return roomMapper.selectPage(page, new LambdaQueryWrapper<Room>()
                .eq(buildingId != null, Room::getBuildingId, buildingId)
                .eq(floorId != null, Room::getFloorId, floorId)
                .like(StringUtils.isNotBlank(roomNumber), Room::getRoomNumber, roomNumber)
                .eq(StringUtils.isNotBlank(status), Room::getStatus, status)
                .orderByAsc(Room::getRoomNumber,Room::getBuildingId, Room::getFloorId, Room::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createRoom(Long buildingId, Long floorId, String roomNumber) {
        validateBuildingAndFloor(buildingId, floorId);
        if (isRoomNumberDuplicate(buildingId, roomNumber, null)) {
            throw new BusinessException("该楼栋下房间号已存在，请修改");
        }
        Room room = new Room();
        room.setBuildingId(buildingId);
        room.setFloorId(floorId);
        room.setRoomNumber(roomNumber);
        room.setStatus("FREE");
        roomMapper.insert(room);
        writeLog("ROOM", "CREATE", "新增房间：" + roomNumber);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(Long id, Long buildingId, Long floorId, String roomNumber, String status) {
        Room room = roomMapper.selectById(id);
        if (room == null) throw new BusinessException("房间不存在");
        validateBuildingAndFloor(buildingId, floorId);
        if (StringUtils.isNotBlank(roomNumber) && !roomNumber.equals(room.getRoomNumber())
                && isRoomNumberDuplicate(buildingId, roomNumber, id)) {
            throw new BusinessException("该楼栋下房间号已存在，请修改");
        }
        if (StringUtils.isNotBlank(status) && !ROOM_STATUSES.contains(status)) {
            throw new BusinessException("无效的房间状态");
        }
        String oldRoomNumber = room.getRoomNumber();
        String oldStatus = room.getStatus();
        room.setBuildingId(buildingId);
        room.setFloorId(floorId);
        if (StringUtils.isNotBlank(roomNumber)) room.setRoomNumber(roomNumber);
        if (StringUtils.isNotBlank(status)) room.setStatus(status);
        roomMapper.updateById(room);
        writeLog("ROOM", "UPDATE", "修改房间：" + oldRoomNumber + " → " + room.getRoomNumber()
                + "，状态：" + oldStatus + " → " + room.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) throw new BusinessException("房间不存在");
        if ("OCCUPIED".equals(room.getStatus())) {
            throw new BusinessException("房间已分配企业，不可删除，请先解除分配");
        }
        roomMapper.deleteById(id);
        writeLog("ROOM", "DELETE", "删除房间：" + room.getRoomNumber());
    }

    // ==================== Internal ====================

    private void validateBuildingAndFloor(Long buildingId, Long floorId) {
        if (buildingMapper.selectById(buildingId) == null) {
            throw new BusinessException("所属楼栋不存在");
        }
        Floor floor = floorMapper.selectById(floorId);
        if (floor == null) {
            throw new BusinessException("所属楼层不存在");
        }
        if (!floor.getBuildingId().equals(buildingId)) {
            throw new BusinessException("楼层不属于指定楼栋");
        }
    }

    private boolean isRoomNumberDuplicate(Long buildingId, String roomNumber, Long excludeId) {
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<Room>()
                .eq(Room::getBuildingId, buildingId)
                .eq(Room::getRoomNumber, roomNumber);
        if (excludeId != null) {
            wrapper.ne(Room::getId, excludeId);
        }
        return roomMapper.selectCount(wrapper) > 0;
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