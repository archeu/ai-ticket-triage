package com.example.triage;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 限流器的单元测试：用一个可以手动拨动的时钟，不用真的等一分钟。 */
class RateLimiterTest {

    @Test
    void blocksAfterPerMinuteLimitAndResetsNextMinute() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(2, 100, 100, clock);

        assertTrue(limiter.tryAcquire("1.1.1.1").isEmpty());
        assertTrue(limiter.tryAcquire("1.1.1.1").isEmpty());
        assertEquals("too_many_per_minute", limiter.tryAcquire("1.1.1.1").orElseThrow().code());
        assertTrue(limiter.tryAcquire("2.2.2.2").isEmpty(), "别的 IP 不受影响");

        clock.advance(Duration.ofMinutes(1));
        assertTrue(limiter.tryAcquire("1.1.1.1").isEmpty(), "下一分钟恢复");
    }

    @Test
    void blocksAfterPerIpDailyLimit() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(100, 2, 100, clock);

        limiter.tryAcquire("1.1.1.1");
        clock.advance(Duration.ofMinutes(5));
        limiter.tryAcquire("1.1.1.1");
        clock.advance(Duration.ofMinutes(5));
        assertEquals("daily_ip_quota", limiter.tryAcquire("1.1.1.1").orElseThrow().code());

        clock.advance(Duration.ofDays(1));
        assertTrue(limiter.tryAcquire("1.1.1.1").isEmpty(), "第二天恢复");
    }

    @Test
    void globalDailyLimitAppliesToEveryone() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(100, 100, 3, clock);

        limiter.tryAcquire("a");
        limiter.tryAcquire("b");
        limiter.tryAcquire("c");
        RateLimiter.Rejection rejection = limiter.tryAcquire("d").orElseThrow();
        assertEquals("daily_global_quota", rejection.code());
        assertTrue(rejection.retryAfterSeconds() > 0);

        clock.advance(Duration.ofDays(1));
        assertTrue(limiter.tryAcquire("d").isEmpty(), "第二天全站额度恢复");
    }

    /** 可以手动往前拨的时钟。 */
    static class MutableClock extends Clock {
        private long millis = Instant.parse("2026-11-02T00:00:00Z").toEpochMilli();

        void advance(Duration duration) {
            millis += duration.toMillis();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
