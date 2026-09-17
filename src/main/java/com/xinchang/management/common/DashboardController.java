package com.xinchang.management.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/api/dashboard/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();

        data.put("buildingCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM building", Long.class));
        data.put("roomTotal", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room", Long.class));
        data.put("roomFree", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room WHERE status = 'FREE'", Long.class));
        data.put("roomOccupied", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room WHERE status = 'OCCUPIED'", Long.class));
        data.put("roomDisabled", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room WHERE status = 'DISABLED'", Long.class));
        data.put("companyTotal", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company", Long.class));
        data.put("companyNormal", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM company WHERE business_status = 'NORMAL'", Long.class));
        data.put("employeeTotal", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employee", Long.class));
        data.put("employeeActive", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employee WHERE status = 'ACTIVE'", Long.class));

        return Result.ok(data);
    }
}