package com.example.triage;

import java.util.Locale;

/**
 * 工单类别。AI 返回的是一段文字，必须校验后再转成枚举，不能直接相信。
 */
public enum Category {
    INQUIRY("咨询"),
    COMPLAINT("投诉"),
    REFUND("退款"),
    TECHNICAL("技术问题"),
    OTHER("其他");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 把 AI 的原始回答转成类别；认不出来就归为 OTHER。 */
    public static Category parse(String raw) {
        if (raw == null) {
            return OTHER;
        }
        String text = raw.trim().toUpperCase(Locale.ROOT);
        for (Category c : values()) {
            if (text.startsWith(c.name())) {
                return c;
            }
        }
        return OTHER;
    }
}
