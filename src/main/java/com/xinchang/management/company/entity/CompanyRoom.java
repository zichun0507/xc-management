package com.xinchang.management.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("company_room")
public class CompanyRoom {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private Long roomId;
    private LocalDateTime allocatedTime;
    private LocalDateTime releasedTime;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDateTime getAllocatedTime() { return allocatedTime; }
    public void setAllocatedTime(LocalDateTime allocatedTime) { this.allocatedTime = allocatedTime; }
    public LocalDateTime getReleasedTime() { return releasedTime; }
    public void setReleasedTime(LocalDateTime releasedTime) { this.releasedTime = releasedTime; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}