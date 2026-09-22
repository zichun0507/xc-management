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

/**
 * 房间业务逻辑服务层，提供楼栋、楼层、房间的增删改查及状态管理
 */
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

    /**
     * 查询所有楼栋列表，按排序序号升序排列
     * @return 楼栋列表
     */
    public List<Building> listBuildings() {
        return buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().orderByAsc(Building::getSortOrder));
    }

    /**
     * 新增楼栋
     * @param name 楼栋名称
     * @param landlord 业主/房东
     * @param sortOrder 排序序号
     */
    @Transactional(rollbackFor = Exception.class)
    public void createBuilding(String name, String landlord, Integer sortOrder) {
        Building building = new Building();
        building.setName(name);
        building.setLandlord(landlord);
        building.setSortOrder(sortOrder != null ? sortOrder : 0);
        buildingMapper.insert(building);
        writeLog("BUILDING", "CREATE", "新增楼栋：" + name);
    }

    /**
     * 更新楼栋信息
     * @param id 楼栋ID
     * @param name 新楼栋名称
     * @param landlord 新业主
     * @param sortOrder 新排序序号
     */
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

    /**
     * 删除楼栋，要求该楼栋下无楼层数据
     * @param id 楼栋ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBuilding(Long id) {
        // 校验楼栋下是否存在楼层，存在则不允许删除
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

    /**
     * 查询指定楼栋下的楼层列表
     * @param buildingId 楼栋ID
     * @return 楼层列表
     */
    public List<Floor> listFloors(Long buildingId) {
        return floorMapper.selectList(
                new LambdaQueryWrapper<Floor>()
                        .eq(buildingId != null, Floor::getBuildingId, buildingId)
                        .orderByAsc(Floor::getSortOrder));
    }

    /**
     * 新增楼层，先校验所属楼栋是否存在
     * @param buildingId 楼栋ID
     * @param name 楼层名称
     * @param sortOrder 排序序号
     */
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

    /**
     * 更新楼层信息
     * @param id 楼层ID
     * @param buildingId 新楼栋ID
     * @param name 新楼层名称
     * @param sortOrder 新排序序号
     */
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

    /**
     * 删除楼层，要求该楼层下无房间数据
     * @param id 楼层ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFloor(Long id) {
        // 校验楼层下是否存在房间，存在则不允许删除
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

    /**
     * 分页查询房间列表，支持按楼栋、楼层、房间号、状态筛选
     * @param buildingId 楼栋ID
     * @param floorId 楼层ID
     * @param roomNumber 房间号
     * @param status 房间状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页房间列表
     */
    public Page<Room> pageRooms(Long buildingId, Long floorId, String roomNumber,
                                   String status, int pageNum, int pageSize) {
        Page<Room> page = new Page<>(pageNum, pageSize);
        return roomMapper.selectPage(page, new LambdaQueryWrapper<Room>()
                .eq(buildingId != null, Room::getBuildingId, buildingId)
                .eq(floorId != null, Room::getFloorId, floorId)
                .like(StringUtils.isNotBlank(roomNumber), Room::getRoomNumber, roomNumber)
                .eq(StringUtils.isNotBlank(status), Room::getStatus, status)
                .orderByAsc(Room::getRoomNumber, Room::getBuildingId, Room::getFloorId, Room::getId));
    }

    /**
     * 新增房间，校验楼栋、楼层存在性及房间号唯一性
     * @param buildingId 楼栋ID
     * @param floorId 楼层ID
     * @param roomNumber 房间号
     */
    @Transactional(rollbackFor = Exception.class)
    public void createRoom(Long buildingId, Long floorId, String roomNumber) {
        validateBuildingAndFloor(buildingId, floorId);
        // 校验同一楼栋下房间号唯一
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

    /**
     * 更新房间信息，包括楼栋、楼层、房间号及状态
     * @param id 房间ID
     * @param buildingId 新楼栋ID
     * @param floorId 新楼层ID
     * @param roomNumber 新房间号
     * @param status 新状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(Long id, Long buildingId, Long floorId, String roomNumber, String status) {
        Room room = roomMapper.selectById(id);
        if (room == null) throw new BusinessException("房间不存在");
        validateBuildingAndFloor(buildingId, floorId);
        // 更新房间号时校验唯一性（排除自身）
        if (StringUtils.isNotBlank(roomNumber) && !roomNumber.equals(room.getRoomNumber())
                && isRoomNumberDuplicate(buildingId, roomNumber, id)) {
            throw new BusinessException("该楼栋下房间号已存在，请修改");
        }
        // 校验状态合法性
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

    /**
     * 删除房间，要求房间未被企业占用
     * @param id 房间ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) throw new BusinessException("房间不存在");
        // 已被占用的房间不可删除，需先解除分配
        if ("OCCUPIED".equals(room.getStatus())) {
            throw new BusinessException("房间已分配企业，不可删除，请先解除分配");
        }
        roomMapper.deleteById(id);
        writeLog("ROOM", "DELETE", "删除房间：" + room.getRoomNumber());
    }

    // ==================== Internal ====================

    /**
     * 校验楼栋和楼层是否存在，且楼层属于指定楼栋
     * @param buildingId 楼栋ID
     * @param floorId 楼层ID
     */
    private void validateBuildingAndFloor(Long buildingId, Long floorId) {
        if (buildingMapper.selectById(buildingId) == null) {
            throw new BusinessException("所属楼栋不存在");
        }
        Floor floor = floorMapper.selectById(floorId);
        if (floor == null) {
            throw new BusinessException("所属楼层不存在");
        }
        // 校验楼层是否属于指定楼栋
        if (!floor.getBuildingId().equals(buildingId)) {
            throw new BusinessException("楼层不属于指定楼栋");
        }
    }

    /**
     * 校验指定楼栋下房间号是否唯一（排除自身ID）
     * @param buildingId 楼栋ID
     * @param roomNumber 房间号
     * @param excludeId 排除的房间ID（更新时使用）
     * @return true表示房间号已重复
     */
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
