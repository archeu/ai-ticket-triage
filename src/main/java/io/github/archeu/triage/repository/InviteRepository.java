package io.github.archeu.triage.repository;

import io.github.archeu.triage.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

/** 邀请码的数据库操作。 */
public interface InviteRepository extends JpaRepository<Invite, String> {
}
