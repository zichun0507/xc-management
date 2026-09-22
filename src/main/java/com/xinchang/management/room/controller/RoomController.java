package com.xinchang.management.room.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.Result;
import com.xinchang.management.room.entity.Building;
import com.xinchang.management.room.entity.Floor;
import com.xinchang.management.room.entity.Room;
import com.xinchang.management.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 房间管理控制器，提供楼栋、楼层、房间资源的CRUD管理接口
 */
@RestController
@Tag(name = "房间管理", description = "楼栋、楼层、房间资源的维护与管理接口")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ==================== Building ====================

    /**
     * 查询所有楼栋列表，按排序序号升序排列
     * @return 楼栋列表
     */
    @GetMapping("/api/buildings")
    @Operation(summary = "查询楼栋列表", description = "获取所有楼栋信息，按排序序号升序排列")
    public Result<List<Building>> listBuildings() {
        return Result.ok(roomService.listBuildings());
    }

    /**
     * 新增楼栋
     * @param body 包含楼栋名称、房东、排序序号
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/api/buildings")
    @Operation(summary = "新增楼栋", description = "创建一个新的楼栋信息")
    public Result<Void> createBuilding(@RequestBody Map<String, Object> body) {
        roomService.createBuilding((String) body.get("name"), (String) body.get("landlord"), (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    /**
     * 更新楼栋信息
     * @param id 楼栋ID
     * @param body 包含更新后的楼栋信息
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/api/buildings/{id}")
    @Operation(summary = "更新楼栋", description = "修改指定楼栋的基本信息")
    public Result<Void> updateBuilding(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        roomService.updateBuilding(id, (String) body.get("name"), (String) body.get("landlord"), (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    /**
     * 删除楼栋（要求该楼栋下无楼层数据）
     * @param id 楼栋ID
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/buildings/{id}")
    @Operation(summary = "删除楼栋", description = "删除指定楼栋，要求该楼栋下无楼层数据")
    public Result<Void> deleteBuilding(@PathVariable Long id) {
        roomService.deleteBuilding(id);
        return Result.ok();
    }

    // ==================== Floor ====================

    /**
     * 查询指定楼栋下的楼层列表
     * @param buildingId 楼栋ID（可选，不传则查询全部）
     * @return 楼层列表
     */
    @GetMapping("/api/floors")
    @Operation(summary = "查询楼层列表", description = "根据楼栋ID查询楼层列表，不传楼栋ID则查询全部")
    public Result<List<Floor>> listFloors(@RequestParam(required = false) Long buildingId) {
        return Result.ok(roomService.listFloors(buildingId));
    }

    /**
     * 新增楼层
     * @param body 包含楼栋ID、楼层名称、排序序号
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/api/floors")
    @Operation(summary = "新增楼层", description = "在指定楼栋下创建一个新楼层")
    public Result<Void> createFloor(@RequestBody Map<String, Object> body) {
        roomService.createFloor(
                Long.valueOf(body.get("buildingId").toString()),
                (String) body.get("name"),
                (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    /**
     * 更新楼层信息
     * @param id 楼层ID
     * @param body 包含更新后的楼层信息
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/api/floors/{id}")
    @Operation(summary = "更新楼层", description = "修改指定楼层的名称及排序")
    public Result<Void> updateFloor(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        roomService.updateFloor(id,
                Long.valueOf(body.get("buildingId").toString()),
                (String) body.get("name"),
                (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    /**
     * 删除楼层（要求该楼层下无房间数据）
     * @param id 楼层ID
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/floors/{id}")
    @Operation(summary = "删除楼层", description = "删除指定楼层，要求该楼层下无房间数据")
    public Result<Void> deleteFloor(@PathVariable Long id) {
        roomService.deleteFloor(id);
        return Result.ok();
    }

    // ==================== Room ====================

    /**
     * 分页查询房间列表，支持按楼栋、楼层、房间号、状态筛选
     * @param buildingId 楼栋ID（可选）
     * @param floorId 楼层ID（可选）
     * @param roomNumber 房间号（可选）
     * @param status 房间状态（可选）
     * @param page 页码
     * @param size 每页条数
     * @return 分页房间列表
     */
    @GetMapping("/api/rooms")
    @Operation(summary = "分页查询房间列表", description = "支持按楼栋、楼层、房间号、状态进行筛选")
    public Result<Page<Room>> pageRooms(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(roomService.pageRooms(buildingId, floorId, roomNumber, status, page, size));
    }

    /**
     * 新增房间（自动校验房间号唯一性）
     * @param body 包含楼栋ID、楼层ID、房间号
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/api/rooms")
    @Operation(summary = "新增房间", description = "在指定楼层下新增一个房间，自动校验房间号唯一性")
    public Result<Void> createRoom(@RequestBody Map<String, Object> body) {
        roomService.createRoom(
                Long.valueOf(body.get("buildingId").toString()),
                Long.valueOf(body.get("floorId").toString()),
                (String) body.get("roomNumber"));
        return Result.ok();
    }

    /**
     * 更新房间信息
     * @param id 房间ID
     * @param body 包含更新后的房间信息
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/api/rooms/{id}")
    @Operation(summary = "更新房间", description = "修改房间信息，包括楼栋、楼层、房间号及状态")
    public Result<Void> updateRoom(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        roomService.updateRoom(id,
                body.get("buildingId") != null ? Long.valueOf(body.get("buildingId").toString()) : null,
                body.get("floorId") != null ? Long.valueOf(body.get("floorId").toString()) : null,
                (String) body.get("roomNumber"),
                (String) body.get("status"));
        return Result.ok();
    }

    /**
     * 删除房间（要求房间未被占用）
     * @param id 房间ID
     * @return 操作结果
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/rooms/{id}")
    @Operation(summary = "删除房间", description = "删除指定房间，要求房间未被企业占用")
    public Result<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return Result.ok();
    }
}
