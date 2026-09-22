package com.xinchang.management.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.employee.entity.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人员附件数据访问层接口
 */
@Mapper
@Schema(description = "人员附件数据访问接口")
public interface AttachmentMapper extends BaseMapper<Attachment> {
}
