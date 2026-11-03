package io.github.archeu.triage.ratelimit;

import io.github.archeu.triage.config.TriageProperties;
import io.github.archeu.triage.config.TriageProperties.RateLimitSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 限流器：按固定时间窗口计数，数据放在内存里（应用重启后清零，单实例的演示站够用）。
 * 三道闸：每个 IP 每分钟、每个 IP 每天、全站每天。
 * 全站每天的上限是 AI 费用的"保险丝"：就算有人换着 IP 刷，一天的调用次数也封顶。
 */
@Component
public class RateLimiter {

    private static final long MINUTE = 60_000L;
    private static final long DAY = 86_400_000L;

    private final RateLimitSettings limits;
    private final Clock clock;

    private final Map<String, Integer> minuteCounts = new HashMap<>();
    private final Map<String, Integer> dayCounts = new HashMap<>();
    private long currentMinute = -1;
    private long currentDay = -1;
    private int globalCount;

    @Autowired
    public RateLimiter(TriageProperties properties) {
        this(properties.rateLimit(), Clock.systemUTC());
    }

    /** 测试用：可以传入一个能手动拨动的时钟。 */
    RateLimiter(RateLimitSettings limits, Clock clock) {
        this.limits = limits;
        this.clock = clock;
    }

    /** 尝试占用一次额度：放行返回空；拒绝返回原因和需要等待的秒数。被拒绝的请求不计数。 */
    public synchronized Optional<Rejection> tryAcquire(String clientIp) {
        long now = clock.millis();
        long minute = now / MINUTE;
        long day = now / DAY; // 按 UTC 自然日重置
        if (minute != currentMinute) {
            minuteCounts.clear(); // 进入新的一分钟，旧的计数整个丢掉，内存不会越积越多
            currentMinute = minute;
        }
        if (day != currentDay) {
            dayCounts.clear();
            globalCount = 0;
            currentDay = day;
        }

        if (globalCount >= limits.globalPerDay()) {
            return Optional.of(new Rejection("daily_global_quota", "今天的演示额度已经用完，请明天再来",
                    secondsUntil((day + 1) * DAY, now)));
        }
        if (minuteCounts.getOrDefault(clientIp, 0) >= limits.perIpPerMinute()) {
            return Optional.of(new Rejection("too_many_per_minute", "请求太频繁，请一分钟后再试",
                    secondsUntil((minute + 1) * MINUTE, now)));
        }
        if (dayCounts.getOrDefault(clientIp, 0) >= limits.perIpPerDay()) {
            return Optional.of(new Rejection("daily_ip_quota", "你今天的试用次数已经用完，请明天再来",
                    secondsUntil((day + 1) * DAY, now)));
        }
        minuteCounts.merge(clientIp, 1, Integer::sum);
        dayCounts.merge(clientIp, 1, Integer::sum);
        globalCount++;
        return Optional.empty();
    }

    private static long secondsUntil(long target, long now) {
        return Math.max(1, (target - now + 999) / 1000);
    }

    /** code 给页面做多语言提示用，message 是中文说明。 */
    public record Rejection(String code, String message, long retryAfterSeconds) {
    }
}
