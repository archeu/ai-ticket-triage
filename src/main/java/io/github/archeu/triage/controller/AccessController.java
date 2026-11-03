package io.github.archeu.triage.controller;

import io.github.archeu.triage.dto.AccessResponse;
import io.github.archeu.triage.service.InviteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** 页面打开时先问一下：需不需要邀请码、手上的邀请码有没有效。 */
@RestController
public class AccessController {

    private final InviteService inviteService;

    public AccessController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/api/access")
    public AccessResponse access(@RequestHeader(value = ApiHeaders.INVITE_CODE, required = false) String inviteCode) {
        return new AccessResponse(inviteService.isRequired(), inviteService.isValid(inviteCode));
    }
}
