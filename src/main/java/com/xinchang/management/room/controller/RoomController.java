package com.xinchang.management.room.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinchang.management.common.Result;
import com.xinchang.management.room.entity.Building;
import com.xinchang.management.room.entity.Floor;
import com.xinchang.management.room.entity.Room;
import com.xinchang.management.room.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ==================== Building ====================

    @GetMapping("/api/buildings")
    public Result<List<Building>> listBuildings() {
        return Result.ok(roomService.listBuildings());
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/api/buildings")
    public Result<Void> createBuilding(@RequestBody Map<String, Object> body) {
        roomService.createBuilding((String) body.get("name"), (String) body.get("landlord"), (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/api/buildings/{id}")
    public Result<Void> updateBuilding(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        roomService.updateBuilding(id, (String) body.get("name"), (String) body.get("landlord"), (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/buildings/{id}")
    public Result<Void> deleteBuilding(@PathVariable Long id) {
        roomService.deleteBuilding(id);
        return Result.ok();
    }

    // ==================== Floor ====================

    @GetMapping("/api/floors")
    public Result<List<Floor>> listFloors(@RequestParam(required = false) Long buildingId) {
        return Result.ok(roomService.listFloors(buildingId));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/api/floors")
    public Result<Void> createFloor(@RequestBody Map<String, Object> body) {
        roomService.createFloor(
                Long.valueOf(body.get("buildingId").toString()),
                (String) body.get("name"),
                (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/api/floors/{id}")
    public Result<Void> updateFloor(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        roomService.updateFloor(id,
                Long.valueOf(body.get("buildingId").toString()),
                (String) body.get("name"),
                (Integer) body.get("sortOrder"));
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/floors/{id}")
    public Result<Void> deleteFloor(@PathVariable Long id) {
        roomService.deleteFloor(id);
        return Result.ok();
    }

    // ==================== Room ====================

    @GetMapping("/api/rooms")
    public Result<Page<Room>> pageRooms(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) String roomNumber,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(roomService.pageRooms(buildingId, floorId, roomNumber, status, page, size));
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/api/rooms")
    public Result<Void> createRoom(@RequestBody Map<String, Object> body) {
        roomService.createRoom(
                Long.valueOf(body.get("buildingId").toString()),
                Long.valueOf(body.get("floorId").toString()),
                (String) body.get("roomNumber"));
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @PutMapping("/api/rooms/{id}")
    public Result<Void> updateRoom(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        roomService.updateRoom(id,
                body.get("buildingId") != null ? Long.valueOf(body.get("buildingId").toString()) : null,
                body.get("floorId") != null ? Long.valueOf(body.get("floorId").toString()) : null,
                (String) body.get("roomNumber"),
                (String) body.get("status"));
        return Result.ok();
    }

    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/rooms/{id}")
    public Result<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return Result.ok();
    }
}