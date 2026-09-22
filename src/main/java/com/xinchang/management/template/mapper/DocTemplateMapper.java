package com.xinchang.management.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.template.entity.DocTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档模板数据访问层接口
 */
@Mapper
@Schema(description = "文档模板数据访问接口")
public interface DocTemplateMapper extends BaseMapper<DocTemplate> {
}
