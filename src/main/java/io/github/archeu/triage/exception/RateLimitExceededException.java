package io.github.archeu.triage.exception;

/** 超过限流上限。code 给页面做多语言提示，retryAfterSeconds 写进 Retry-After 响应头。 */
public class RateLimitExceededException extends RuntimeException {

    private final String code;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String code, String message, long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getCode() {
        return code;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
