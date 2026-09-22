package com.xinchang.management.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinchang.management.employee.entity.Employee;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业人员数据访问层接口
 */
@Mapper
@Schema(description = "企业人员数据访问接口")
public interface EmployeeMapper extends BaseMapper<Employee> {
}
