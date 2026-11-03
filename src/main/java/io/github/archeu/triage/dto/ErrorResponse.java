package io.github.archeu.triage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一的错误响应。
 *
 * @param error 给人看的说明（中文）
 * @param code  错误码，页面用它查多语言提示；没有时不输出这个字段
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String code) {
}
