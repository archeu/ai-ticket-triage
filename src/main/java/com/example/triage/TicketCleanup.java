package com.example.triage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 公开演示站的数据定时清理：访客提交的内容所有人可见，只保留几天。
 * triage.retention-days=0 表示不清理（本地开发用）。
 */
@Component
public class TicketCleanup {

    private static final Logger log = LoggerFactory.getLogger(TicketCleanup.class);

    private final TicketRepository repository;
    private final int retentionDays;

    public TicketCleanup(TicketRepository repository, @Value("${triage.retention-days}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
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
