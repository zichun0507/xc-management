package com.xinchang.management.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器，用于监控系统运行状态
 */
@RestController
@Schema(description = "健康检查控制器")
public class HealthController {

    /**
     * 检查服务是否正常运行
     * @return 服务状态信息
     */
    @GetMapping("/api/health")
    @Schema(description = "健康检查接口")
    public Result<String> health() {
        return Result.ok("UP");
    }
}
