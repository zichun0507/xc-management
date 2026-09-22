package com.xinchang.management.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.company.entity.Company;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业数据访问层接口，继承 BaseMapper 提供通用 CRUD 操作
 */
@Mapper
@Schema(description = "企业数据访问接口")
public interface CompanyMapper extends BaseMapper<Company> {
}
