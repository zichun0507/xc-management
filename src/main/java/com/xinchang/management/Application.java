package com.xinchang.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 应用启动类，扫描所有Mapper接口并启动Spring Boot应用
 */
@SpringBootApplication
@MapperScan("com.xinchang.management.**.mapper")
@Schema(description = "应用启动类")
public class Application {

    /**
     * 应用入口方法
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
