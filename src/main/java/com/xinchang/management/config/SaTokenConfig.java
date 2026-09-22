package com.xinchang.management.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * sa-token 配置类，配置登录拦截器，对 /api/** 路径进行登录校验
 */
@Configuration
@Schema(description = "sa-token配置类")
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册 sa-token 拦截器，对 API 路径进行登录状态校验
     * 排除登录接口和健康检查接口
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/api/**")
                    .notMatch("/api/auth/login", "/api/health")
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/api/**");
    }
}
