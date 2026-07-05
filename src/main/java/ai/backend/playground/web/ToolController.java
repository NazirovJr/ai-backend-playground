package ai.backend.playground.web;

import ai.backend.playground.tools.SupportTools;
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
 * Урок 1.4 — Tool / Function Calling.
 * Передаём модели инструменты через .tools(...). Дальше модель сама решает,
 * вызывать ли их. Spring AI 2.0 выполняет весь tool-loop автоматически.
 */
@RestController
@RequestMapping("/tools")
public class ToolController {

    private final ChatClient chatClient;
    private final SupportTools supportTools;

    public ToolController(ChatClient.Builder builder, SupportTools supportTools, ChatMemory chatMemory) {
        this.chatClient = builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
        this.supportTools = supportTools;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .tools(supportTools)   // <-- даём модели доступ к инструментам
                .call()
                .content();
    }

    @GetMapping("/weather")
    public String getWeather(@RequestParam String message, @RequestParam(defaultValue = "defalt") String conversationId) {
        return chatClient.prompt()
                .user(message)
                .tools(supportTools)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
