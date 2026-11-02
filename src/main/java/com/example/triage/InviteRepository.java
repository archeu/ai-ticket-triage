package com.example.triage;

import org.springframework.data.jpa.repository.JpaRepository;

/** 邀请码的数据库操作。 */
public interface InviteRepository extends JpaRepository<Invite, String> {
}
