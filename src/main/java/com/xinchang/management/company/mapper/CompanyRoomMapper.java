package com.xinchang.management.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.company.entity.CompanyRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业-房间关联数据访问层接口
 */
@Mapper
@Schema(description = "企业房间关联数据访问接口")
public interface CompanyRoomMapper extends BaseMapper<CompanyRoom> {
}
