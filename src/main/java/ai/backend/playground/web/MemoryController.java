package ai.backend.playground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Урок 1.5 — Chat Memory.
 * Ассистент помнит историю диалога. Память привязана к conversationId,
 * поэтому разные пользователи/сессии не смешиваются.
 */
@RestController
@RequestMapping("/memory")
public class MemoryController {

    private final ChatClient chatClient;

    // ChatMemory внедряется как ОБЩИЙ бин (см. AiConfig) — не создаём свой.
    public MemoryController(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultSystem("Ты — дружелюбный ассистент. Отвечай кратко, по-русски.")
                // Advisor сам подкладывает историю диалога в каждый запрос
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = "default") String conversationId) {
        return chatClient.prompt()
                .user(message)
                // В Spring AI 2.0 conversationId ОБЯЗАТЕЛЕН для memory-advisor
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
