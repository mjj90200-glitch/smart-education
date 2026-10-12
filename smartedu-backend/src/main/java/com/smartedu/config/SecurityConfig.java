package com.smartedu.config;

import com.smartedu.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 配置类
 * <p>
 * 安全策略说明：
 * 1. 无状态 Session（STATELESS）
 * 2. 禁用 CSRF（前后端分离架构）
 * 3. 配置 CORS 跨域
 * 4. 支持方法级权限控制（@PreAuthorize）
 * 5. 集成 JWT 认证过滤器
 * 6. 支持 ASYNC 分派（流式响应需要）
 * 7. SecurityContext 线程继承（异步线程池需要）
 * </p>
 *
 * @author SmartEdu Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // 开启方法级安全控制，支持 @PreAuthorize 注解
public class SecurityConfig {

    // 静态初始化：设置 SecurityContext 策略为可继承线程本地
    // 这使得主线程的 SecurityContext 能自动传递给异步线程（如 boundedElastic 线程池）
    static {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 自定义 AccessDeniedHandler - 处理流式响应中的权限错误
     * 当响应已提交时，避免抛出异常导致 Servlet 错误
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                    AccessDeniedException accessDeniedException) throws IOException, ServletException {
                // 如果响应已提交，不再处理
                if (response.isCommitted()) {
                    return;
                }
                // 设置错误响应
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"code\":403,\"message\":\"权限不足\"}");
            }
        };
    }

    /**
     * 密码编码器
     * 使用 BCrypt 加密算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤链配置
     * <p>
     * 路径权限说明：
     * - /swagger-ui/**, /v3/api-docs/** : API 文档，无需认证
     * - /auth/** : 登录注册，无需认证
     * - /news/carousel, /news/list, /news/{id} : 新闻查询，无需认证
     * - /news/manual-update-dev : 开发测试接口，无需认证（生产环境应删除）
     * - /homework/** : 作业模块，需要 TEACHER/STUDENT/ADMIN 角色
     * - /forum/** : 答疑大厅，需要认证（所有登录用户）
     * - 其他请求 : 需要认证
     * </p>
     *
     * 注意：由于 server.servlet.context-path=/api，Spring Security 会自动处理 context-path，
     * 所以 requestMatchers 中的路径不需要 /api 前缀
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ========== 配置 CORS（必须在 CSRF 之前）==========
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 禁用 CSRF（前后端分离架构不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用 Frame（H2 控制台需要）
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            // 无状态 Session
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // ========== 关键：允许 ASYNC 分派（流式响应需要）==========
                // Spring Security 需要信任由已授权请求发起的内部异步分派
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                // ========== 放行 OPTIONS 预检请求（CORS 需要）==========
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // ========== 放行静态资源和 API 文档 ==========
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // ========== 放行 Actuator 健康检查端点 ==========
                .requestMatchers("/actuator/**").permitAll()
                // ========== 放行认证相关接口 ==========
                .requestMatchers("/auth/**", "/user/register", "/user/login").permitAll()
                // ========== 放行 H2 控制台（开发用）==========
                .requestMatchers("/h2-console/**").permitAll()
                // ========== 放行用户头像访问（静态资源）==========
                .requestMatchers("/user/avatar/**").permitAll()
                // ========== 放行新闻图片访问（静态资源）==========
                .requestMatchers("/news/image/**").permitAll()
                // ========== 放行上传文件访问（静态资源）==========
                .requestMatchers("/uploads/**").permitAll()
                // ========== 放行新闻查询接口（公开访问）==========
                .requestMatchers("/news/carousel", "/news/list", "/news/{id}").permitAll()
                // ========== 放行推荐视频接口（公开访问）==========
                .requestMatchers("/recommend-videos/**").permitAll()
                // ========== 放行视频学习接口（公开访问）==========
                .requestMatchers("/video/home", "/video/list").permitAll()
                .requestMatchers("/video/*").permitAll()
                // ========== 放行首页推荐接口（公开访问）==========
                .requestMatchers("/home-recommend/list").permitAll()
                // ========== 放行开发测试接口（生产环境应删除）==========
                .requestMatchers("/news/manual-update-dev").permitAll()
                // ========== AI Agent 接口：暂时允许公开访问（测试用）==========
                .requestMatchers("/ai/**").permitAll()
                // ========== 作业模块：需要 TEACHER/STUDENT/ADMIN 角色 ==========
                // 使用 hasAuthority 匹配完整的 authority（包括 ROLE_ 前缀）
                .requestMatchers("/homework/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_STUDENT", "ROLE_ADMIN")
                // ========== 其他请求需要认证 ==========
                .anyRequest().authenticated()
            )
            // 配置异常处理 - 处理流式响应中的权限错误
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS 配置
     * <p>
     * 注意：当前允许所有来源，生产环境应限制具体域名
     * </p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源（生产环境应限制具体域名）
        configuration.setAllowedOriginPatterns(List.of("*"));
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许携带凭证（Cookie、Authorization 头等）
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}