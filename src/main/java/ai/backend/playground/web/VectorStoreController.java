package ai.backend.playground.web;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Урок 2.2 — pgvector.
 * Храним документы (как векторы) в PostgreSQL и ищем по смыслу.
 * Spring AI сам эмбеддит текст через bge-m3 при добавлении и при поиске.
 */
@RestController
@RequestMapping("/vectors")
public class VectorStoreController {

    private final VectorStore vectorStore;

    public VectorStoreController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    private List<Document> loadMDDocument(String path) {
        Resource resource = new ClassPathResource(path);
        MarkdownDocumentReaderConfig markdownDocumentReaderConfig = MarkdownDocumentReaderConfig.builder().
                withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(true)
                .build();

        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, markdownDocumentReaderConfig);

        return markdownDocumentReader.get();
    }

    // Засеваем мини-базу знаний (по-хорошему это POST; GET — чтобы удобно дёргать curl).
    @GetMapping("/seed")
    public String seed() {
        List<Document> docs = new ArrayList<>(List.of(
                new Document("Чтобы сбросить пароль, откройте страницу входа и нажмите «Забыли пароль» — ссылка для сброса придёт на вашу почту.", Map.of("category", "account")),
                new Document("Поддержка работает с 9:00 до 18:00 по будням, кроме выходных и государственных праздников.", Map.of("category", "support")),
                new Document("Возврат средств оформляется в течение 14 дней с момента покупки при обращении в поддержку.", Map.of("category", "billing")),
                new Document("Чтобы сменить тарифный план, зайдите в Настройки, откройте раздел «Подписка» и выберите новый тариф.", Map.of("category", "billing"))
        ));

        docs.addAll(loadMDDocument("HELP.md"));
        docs.addAll(loadMDDocument("docs/policy.md"));
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(100)
                .withMinChunkSizeChars(100)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(1000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = tokenTextSplitter.split(docs);

        vectorStore.add(chunks);
        return "Добавлено документов: " + chunks.size();
    }

    // Поиск по смыслу: возвращаем top-K самых близких документов.
    @GetMapping("/search")
    public List<String> search(@RequestParam String query) {
        return vectorStore.similaritySearch(
                        SearchRequest.builder().query(query).similarityThreshold(0.5).topK(2).build())
                .stream()
                .map(Document::getText)
                .toList();
    }

    @GetMapping("/search-filtered")
    public List<String> searchFiltered(@RequestParam String query, @RequestParam String category) {
        var categoryExpression = new FilterExpressionBuilder();
        Filter.Expression expression = categoryExpression.eq("category", category).build();

        return vectorStore.similaritySearch(
                        SearchRequest.builder().query(query)
                                .similarityThreshold(0.3)
                                .topK(3)
                                .filterExpression(expression)
                                .build()
                ).stream()
                .map(Document::getText)
                .toList();
    }
}
