package io.github.archeu.triage.exception;

/** 邀请码不通过：没带、无效或今天用完。code 给页面做多语言提示用。 */
public class InviteRejectedException extends RuntimeException {

    private final String code;

    public InviteRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
