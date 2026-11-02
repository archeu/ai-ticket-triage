package com.example.triage;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 邀请码每天的使用上限：用完当天拒绝，第二天自动恢复。 */
class InviteTest {

    @Test
    void dailyLimitResetsNextDay() {
        Invite invite = new Invite("abc123", "测试客户", 2);
        LocalDate day1 = LocalDate.of(2026, 11, 2);
        Instant now = Instant.parse("2026-11-02T08:00:00Z");

        assertTrue(invite.tryUse(day1, now));
        assertTrue(invite.tryUse(day1, now));
        assertFalse(invite.tryUse(day1, now), "当天第 3 次应该被拒绝");

        assertTrue(invite.tryUse(day1.plusDays(1), now), "第二天恢复");
        assertEquals(3, invite.getUseCount(), "累计使用次数只算成功的");
    }
}
