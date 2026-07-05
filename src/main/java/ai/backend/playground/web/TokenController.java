package ai.backend.playground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Урок 1.6 — Observability и стоимость.
 * Достаём из ответа модели счётчики токенов и прикидываем стоимость.
 */
@RestController
@RequestMapping("/metrics-demo")
public class TokenController {

    private final ChatClient chatClient;

    public TokenController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public record TokenReport(
            String answer,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            double estimatedCostUsdIfCloud) {}

    @GetMapping("/ask")
    public TokenReport ask(@RequestParam String message) {
        // .chatResponse() (вместо .content()) даёт ПОЛНЫЙ ответ с метаданными
        ChatResponse response = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();

        String answer = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();

        long promptTokens = usage.getPromptTokens();
        long completionTokens = usage.getCompletionTokens();
        long totalTokens = usage.getTotalTokens();

        // Локально (Ollama) это бесплатно. Но прикинем, СКОЛЬКО бы стоило в облаке
        // по типичным ценам (~$0.15 за 1M входных, ~$0.60 за 1M выходных токенов).
        double cost = promptTokens / 1_000_000.0 * 0.15
                + completionTokens / 1_000_000.0 * 0.60;

        return new TokenReport(answer, promptTokens, completionTokens, totalTokens, cost);
    }
}
