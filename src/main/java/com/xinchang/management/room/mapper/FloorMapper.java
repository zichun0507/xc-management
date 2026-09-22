package com.xinchang.management.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.room.entity.Floor;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 楼层数据访问层接口
 */
@Mapper
@Schema(description = "楼层数据访问接口")
public interface FloorMapper extends BaseMapper<Floor> {
}
