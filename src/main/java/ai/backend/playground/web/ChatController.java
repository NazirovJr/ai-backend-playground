package ai.backend.playground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Первый живой вызов LLM из кода.
 * GET /chat?message=...  ->  ответ локальной модели gemma4 через Ollama.
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;

    // ChatClient.Builder автоматически настраивается Spring AI,
    // потому что в classpath есть Ollama-стартер и сконфигурирована модель.
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "Привет! Ответь одним предложением, кто ты.") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
