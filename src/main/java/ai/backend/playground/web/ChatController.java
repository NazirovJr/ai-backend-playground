package ai.backend.playground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Урок 1.1 — ChatClient глубоко.
 * Демонстрирует: системный промпт, переопределение на запрос,
 * управление температурой и стриминг.
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                // Системный промпт задаёт "характер" ассистента для ВСЕХ запросов.
                // Заодно просим модель не выводить рассуждения вслух.
                .defaultSystem("""
                                Ты — лаконичный технический ассистент. Отвечай по-русски, без рассуждений вслух.
                                В САМОМ КОНЦЕ каждого ответа добавляй отдельной строкой: [SYS-DEFAULT].
                        """)
                .build();
    }

    // 1) Обычный синхронный ответ — ждём весь ответ целиком.
    @GetMapping
    public String chat(@RequestParam(defaultValue = "Кто ты?") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    // 2) Переопределяем системный промпт ТОЛЬКО для этого запроса.
    //    Запрос-уровень перекрывает defaultSystem.
    @GetMapping("/as")
    public String chatAs(@RequestParam String role,
                         @RequestParam String message) {
        return chatClient.prompt()
                .system("Отвечай в роли: " + role + ". Кратко, в этом стиле.")
                .user(message)
                .call()
                .content();
    }

    // 3) Управляем "креативностью" на лету через options.
    //    temperature: 0.0 = детерминированно, 1.0+ = креативно.
    @GetMapping("/creative")
    public String creative(@RequestParam String message,
                           @RequestParam(defaultValue = "0.9") double temperature) {
        return chatClient.prompt()
                .user(message)
                // В Spring AI 2.0 .options() принимает БИЛДЕР (без .build()) —
                // фреймворк сам домержит его к дефолтным опциям модели.
                .options(ChatOptions.builder().temperature(temperature))
                .call()
                .content();
    }

    // 4) Стриминг — токены приходят по мере генерации (как печатает ChatGPT).
    @GetMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> stream(
            @RequestParam(defaultValue = "Объясни, что такое REST API, за 5 предложений") String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
