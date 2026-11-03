package io.github.archeu.triage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交一封邮件做 AI 分析。
 *
 * @param email           客户邮件原文；限制长度，防止超长内容浪费 AI 额度
 * @param summaryLanguage 摘要用的语言："zh"（默认）或 "en"，跟着客服看的界面语言走
 */
public record CreateTicketRequest(
        @NotBlank(message = "邮件内容不能为空")
        @Size(max = 8000, message = "邮件太长，最多 8000 个字符")
        String email,

        String summaryLanguage) {
}
