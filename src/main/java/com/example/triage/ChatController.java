package com.example.triage;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一个 AI 接口：输入一句话，返回 AI 的回答。
 * 做的事和 curl 测试一样，只是换成了 Java：Spring AI 帮你拼请求、发请求、解析 JSON。
 * 只在本地开发（dev 配置）时启用：公开出去就成了任何人都能用的免费 AI 接口，花的是你的钱。
 */
@Profile("dev")
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    // Spring AI 按 application.properties 自动配置好 ChatClient.Builder，注入后 build 一下就能用
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chat")
    public ChatReply chat(@RequestBody ChatRequest request) {
        String answer = chatClient.prompt()
                .user(request.message()) // 用户说的话，对应 messages 里 role=user 的那条
                .call()                  // 发出 HTTP 请求，等待返回
                .content();              // 取出 choices[0].message.content
        return new ChatReply(answer);
    }

    public record ChatRequest(String message) {
    }

    public record ChatReply(String answer) {
    }
}
