package io.github.archeu.triage.exception;

/** AI 调用失败，或返回的内容无法解析成结构化结果。 */
public class AnalysisFailedException extends RuntimeException {

    public AnalysisFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
