package com.xinchang.management.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ?", Long.class, "admin");
        if (count == null || count == 0) {
            String encodedPwd = passwordEncoder.encode("admin123");
            jdbcTemplate.update(
                    "INSERT INTO sys_user (username, password, real_name, role, status) VALUES (?, ?, ?, 'ADMIN', 'ENABLED')",
                    "admin", encodedPwd, "管理员");
            log.info("默认管理员已创建: admin / admin123");
        } else {
            log.info("管理员账号已存在，跳过初始化");
        }
    }
}