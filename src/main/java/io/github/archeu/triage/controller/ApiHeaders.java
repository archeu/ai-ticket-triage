package io.github.archeu.triage.controller;

/**
 * 接口用到的自定义请求头。
 * 邀请码和访客 ID 放在请求头里而不是网址里，避免被写进访问日志。
 */
public final class ApiHeaders {

    /** 邀请码，来自邀请链接 ?invite=xxx，页面保存在浏览器里。 */
    public static final String INVITE_CODE = "X-Invite-Code";
    /** 访客 ID，浏览器第一次打开页面时随机生成，用来实现"每个访客只看自己的工单"。 */
    public static final String VISITOR_ID = "X-Visitor-Id";

    private ApiHeaders() {
    }
}
