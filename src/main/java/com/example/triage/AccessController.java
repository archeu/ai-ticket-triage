package com.example.triage;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 页面打开时先问一下：需不需要邀请码、手上的邀请码有没有效。
 * 邀请码放在请求头里而不是网址里，避免被写进访问日志。
 */
@RestController
public class AccessController {

    private final InviteService inviteService;

    public AccessController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/api/access")
    public Map<String, Boolean> access(@RequestHeader(value = "X-Invite-Code", required = false) String inviteCode) {
        return Map.of("required", inviteService.isRequired(), "valid", inviteService.isValid(inviteCode));
    }
}
