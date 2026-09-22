package com.xinchang.management.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.room.entity.Room;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房间数据访问层接口
 */
@Mapper
@Schema(description = "房间数据访问接口")
public interface RoomMapper extends BaseMapper<Room> {
}
