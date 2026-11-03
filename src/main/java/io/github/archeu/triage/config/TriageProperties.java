package io.github.archeu.triage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用自己的配置项，对应 application.properties 里 triage. 开头的部分。
 * 集中放在一个类里：类型安全，启动时就能发现配置写错，不用在各处写 @Value("${...}")。
 *
 * @param rateLimit     限流上限
 * @param invite        邀请码设置
 * @param retentionDays 普通工单保留几天，0 表示不清理（本地开发用）
 */
@ConfigurationProperties(prefix = "triage")
public record TriageProperties(RateLimitSettings rateLimit, InviteSettings invite, int retentionDays) {

    /** 每个 IP 每分钟、每个 IP 每天、全站每天最多几次 AI 调用。 */
    public record RateLimitSettings(int perIpPerMinute, int perIpPerDay, int globalPerDay) {
    }

    /** required=false 时不校验邀请码（本地开发用）。 */
    public record InviteSettings(boolean required) {
    }
}
