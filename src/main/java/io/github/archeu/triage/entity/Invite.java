package io.github.archeu.triage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 邀请码：只有拿到邀请链接的人才能使用 AI 分析。
 * 每个客户发一个，可以单独停用，有每天的使用上限。用 scripts/invite.sh 管理（直接操作数据库，应用里没有管理接口，减少被攻击的入口）。
 */
@Entity
@Table(name = "invites")
public class Invite {

    @Id
    @Column(length = 32)
    private String code;

    /** 这个链接发给了谁，例如"Upwork 客户 Acme"。 */
    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int dailyLimit;

    @Column(nullable = false)
    private int useCount;

    @Column(nullable = false)
    private int usesToday;

    private LocalDate usesDay;

    private Instant lastUsedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected Invite() {
        // JPA 需要一个无参构造方法
    }

    public Invite(String code, String label, int dailyLimit) {
        this.code = code;
        this.label = label;
        this.active = true;
        this.dailyLimit = dailyLimit;
        this.createdAt = Instant.now();
    }

    /** 占用一次当天额度：成功返回 true；今天用完了返回 false。跨天自动清零。 */
    public boolean tryUse(LocalDate today, Instant now) {
        if (!today.equals(usesDay)) {
            usesDay = today;
            usesToday = 0;
        }
        if (usesToday >= dailyLimit) {
            return false;
        }
        usesToday++;
        useCount++;
        lastUsedAt = now;
        return true;
    }

    public boolean isActive() {
        return active;
    }

    public int getUseCount() {
        return useCount;
    }
}
