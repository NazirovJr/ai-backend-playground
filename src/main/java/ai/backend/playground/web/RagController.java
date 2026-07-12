package ai.backend.playground.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Урок 2.4 — настоящий RAG.
 * QuestionAnswerAdvisor на каждый вопрос: эмбеддит его, ищет релевантные куски
 * в VectorStore и подкладывает их в промпт. Модель отвечает по найденному.
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final ChatClient chatClient;

    public RagController(ChatClient.Builder builder, VectorStore vectorStore) {
        // Advisor сам делает retrieval: порог 0.5, топ-3 куска.
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.5)
                        .topK(3)
                        .build())
                .build();

        this.chatClient = builder
                // Заземление (grounding): отвечать ТОЛЬКО по контексту, не выдумывать.
                .defaultSystem("""
                        Ты — ассистент поддержки. Отвечай на вопрос ТОЛЬКО на основе
                        предоставленного контекста. Если ответа в контексте нет —
                        честно скажи, что информации нет. Не придумывай.
                        Отвечай кратко, по-русски.
                        """)
                .defaultAdvisors(qaAdvisor)
                .build();
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
