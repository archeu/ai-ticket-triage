package io.github.archeu.triage.exception;

import io.github.archeu.triage.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一的错误处理：所有接口出错时都返回 {"error": "说明", "code": "错误码"}。
 * code 给页面查多语言提示用；没有 code 的错误，页面直接显示 error。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 请求参数校验不通过（例如邮件为空或太长）：400。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> onInvalidRequest(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getFieldError();
        String message = error != null ? error.getDefaultMessage() : "请求参数不正确";
        return ResponseEntity.badRequest().body(new ErrorResponse(message, null));
    }

    /** 没有邀请码、邀请码无效或今天用完：403。 */
    @ExceptionHandler(InviteRejectedException.class)
    public ResponseEntity<ErrorResponse> onInviteRejected(InviteRejectedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage(), e.getCode()));
    }

    /** 超过限流：429，并用 Retry-After 告诉对方多少秒后再试。 */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> onRateLimited(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(new ErrorResponse(e.getMessage(), e.getCode()));
    }

    /** AI 出错：502。详细原因只写日志，不返回给前端（可能带着模型服务商的内部信息）。 */
    @ExceptionHandler(AnalysisFailedException.class)
    public ResponseEntity<ErrorResponse> onAnalysisFailed(AnalysisFailedException e) {
        log.warn("AI 分析失败", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("AI 分析失败，请稍后再试", "analysis_failed"));
    }
}
