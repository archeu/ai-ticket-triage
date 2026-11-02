package com.example.triage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;

/**
 * 工单的数据库操作：保存、按 id 查询等，Spring Data JPA 自动实现。
 * JpaSpecificationExecutor 用来做"条件可有可无"的组合筛选。
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    /** 删除某个时间之前创建的普通工单（示例除外），返回删除的条数。方法名就是查询条件，Spring Data 自动实现。 */
    long deleteBySampleLangIsNullAndCreatedAtBefore(Instant cutoff);
}
