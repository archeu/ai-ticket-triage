package io.github.archeu.triage.job;

import io.github.archeu.triage.config.TriageProperties;
import io.github.archeu.triage.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 定时清理：公开演示站上访客提交的内容只保留几天（公开示例不删）。
 * triage.retention-days=0 表示不清理（本地开发用）。
 */
@Component
public class TicketCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TicketCleanupJob.class);

    private final TicketRepository repository;
    private final int retentionDays;

    public TicketCleanupJob(TicketRepository repository, TriageProperties properties) {
        this.repository = repository;
        this.retentionDays = properties.retentionDays();
    }

    /** 每小时整点检查一次。 */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteOldTickets() {
        if (retentionDays <= 0) {
            return;
        }
        long deleted = repository.deleteBySampleLangIsNullAndCreatedAtBefore(
                Instant.now().minus(Duration.ofDays(retentionDays)));
        if (deleted > 0) {
            log.info("已删除 {} 条超过 {} 天的工单", deleted, retentionDays);
        }
    }
}
