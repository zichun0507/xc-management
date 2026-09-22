package com.xinchang.management.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 应用全局配置类，定义 BCrypt 密码编码器 Bean
 */
@Configuration
public class AppConfig {

    /**
     * 创建 BCryptPasswordEncoder 实例，用于密码的加密与校验
     * @return BCryptPasswordEncoder 编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
