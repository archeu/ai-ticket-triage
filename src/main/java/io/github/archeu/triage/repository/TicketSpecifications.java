package io.github.archeu.triage.repository;

import io.github.archeu.triage.entity.Category;
import io.github.archeu.triage.entity.Ticket;
import io.github.archeu.triage.entity.Urgency;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** 工单列表的查询条件。条件可有可无时，用 Specification 动态拼，比写一堆 if + JPQL 清楚。 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    /**
     * 访客能看到的范围：当前语言的公开示例 + 自己提交的工单；再按类别、紧急程度筛选（为 null 就不筛）。
     *
     * @param visitorId 已校验过格式的访客 ID；为 null 时只能看到示例
     */
    public static Specification<Ticket> visibleTo(String sampleLang, String visitorId,
                                                  Category category, Urgency urgency) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            Predicate samples = cb.equal(root.get("sampleLang"), sampleLang);
            conditions.add(visitorId != null ? cb.or(samples, cb.equal(root.get("visitorId"), visitorId)) : samples);
            if (category != null) {
                conditions.add(cb.equal(root.get("category"), category));
            }
            if (urgency != null) {
                conditions.add(cb.equal(root.get("urgency"), urgency));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
    }
}
