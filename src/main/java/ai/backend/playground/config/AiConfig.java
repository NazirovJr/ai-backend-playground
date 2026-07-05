package ai.backend.playground.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Общая конфигурация AI-компонентов.
 */
@Configuration
public class AiConfig {

    /**
     * ОДИН общий экземпляр памяти на всё приложение.
     * Любой контроллер, который его внедрит, делит одну историю (по conversationId),
     * поэтому диалог продолжается между разными эндпоинтами.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }
}
