package com.example.triage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * 邀请码校验。triage.invite.required=false 时不校验（本地开发用）。
 */
@Service
public class InviteService {

    private final InviteRepository repository;
    private final boolean required;

    public InviteService(InviteRepository repository, @Value("${triage.invite.required}") boolean required) {
        this.repository = repository;
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    /** 只检查邀请码是否有效，不占用额度（页面打开时用来决定显示什么）。 */
    public boolean isValid(String code) {
        if (!required) {
            return true;
        }
        return code != null && repository.findById(code.trim()).map(Invite::isActive).orElse(false);
    }

    /** 校验并占用一次邀请码的当天额度；不通过就抛异常，后面的 AI 调用不会发生。 */
    @Transactional
    public void consume(String code) {
        if (!required) {
            return;
        }
        if (code == null || code.isBlank()) {
            throw new InviteRejectedException("invite_required", "需要邀请链接才能使用 AI 分析");
        }
        Invite invite = repository.findById(code.trim())
                .filter(Invite::isActive)
                .orElseThrow(() -> new InviteRejectedException("invite_invalid", "邀请链接无效或已停用"));
        if (!invite.tryUse(LocalDate.now(ZoneOffset.UTC), Instant.now())) {
            throw new InviteRejectedException("invite_daily_limit", "这个邀请链接今天的使用次数已用完，请明天再试");
        }
        // invite 是 JPA 托管对象，事务提交时自动把使用次数写回数据库
    }

    /** 邀请码不通过。code 给页面做多语言提示用。 */
    public static class InviteRejectedException extends RuntimeException {
        private final String code;

        public InviteRejectedException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
