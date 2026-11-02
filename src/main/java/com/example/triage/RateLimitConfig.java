package com.example.triage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * 把限流器挂到所有 POST /api/** 请求上（只有提交会调用 AI，查看列表不限）。
 * 超限返回 429 Too Many Requests 和 Retry-After 头。
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimiter rateLimiter;

    public RateLimitConfig(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws IOException {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    return true;
                }
                Optional<RateLimiter.Rejection> rejection = rateLimiter.tryAcquire(clientIp(request));
                if (rejection.isEmpty()) {
                    return true;
                }
                RateLimiter.Rejection r = rejection.get();
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(r.retryAfterSeconds()));
                response.setContentType("application/json;charset=UTF-8");
                // code 和 message 都是代码里写死的常量，不含引号，可以直接拼 JSON
                response.getWriter().write("{\"error\":\"" + r.message() + "\",\"code\":\"" + r.code() + "\"}");
                return false;
            }
        }).addPathPatterns("/api/**");
    }

    /**
     * 取访客的真实 IP。
     * 线上链路是 访客 → Cloudflare → Caddy → Docker 里的应用，应用直接看到的是 Docker 网关的内网地址。
     * Caddy 会把它算出的真实 IP 放进 X-Real-IP 头。只有请求来自本机或内网（也就是 Caddy）时才相信这个头；
     * 外网直接发来的 X-Real-IP 一律忽略，防止有人伪造 IP 绕过限流。
     */
    static String clientIp(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && isLocalOrPrivate(peer)) {
            return realIp.trim();
        }
        return peer;
    }

    private static boolean isLocalOrPrivate(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip); // ip 是数字地址，不会触发 DNS 查询
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
