package io.github.archeu.triage.ratelimit;

import io.github.archeu.triage.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 限流拦截器：只拦 POST 请求（只有提交会调用 AI，查看列表不限）。
 * 超限时抛出 RateLimitExceededException，由 GlobalExceptionHandler 统一返回 429 和 Retry-After。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    public RateLimitInterceptor(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            rateLimiter.tryAcquire(clientIp(request)).ifPresent(r -> {
                throw new RateLimitExceededException(r.code(), r.message(), r.retryAfterSeconds());
            });
        }
        return true;
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
