package io.github.archeu.triage.entity;

/** 工单类别。各类别的中英文名称在前端页面里（js/app.js）。 */
public enum Category {
    /** 咨询：询问商品、价格、物流进度、使用方法等 */
    INQUIRY,
    /** 投诉：对商品质量、服务或态度表达不满，但没有要求退款 */
    COMPLAINT,
    /** 退款：要求退货、退款或换货 */
    REFUND,
    /** 技术问题：网站、App、支付、账号登录等出现故障 */
    TECHNICAL,
    /** 其他 */
    OTHER
}
