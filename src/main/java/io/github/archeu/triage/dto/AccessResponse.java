package io.github.archeu.triage.dto;

/**
 * 访问状态：页面打开时用来决定显示输入框还是"需要邀请链接"的提示。
 *
 * @param required 是否需要邀请码
 * @param valid    当前邀请码是否有效（不需要邀请码时总是 true）
 */
public record AccessResponse(boolean required, boolean valid) {
}
