package ai.backend.playground.web;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Урок 2.1 — эмбеддинги и векторный поиск.
 * EmbeddingModel превращает текст в вектор (массив чисел), отражающий СМЫСЛ.
 * Близкие по смыслу тексты дают близкие векторы (высокая косинусная близость).
 */
@RestController
@RequestMapping("/embeddings")
public class EmbeddingController {

    private final EmbeddingModel embeddingModel;

    public EmbeddingController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public record VectorInfo(String text, int dimensions, List<Float> preview) {}

    public record SimilarityResult(String textA, String textB, double cosineSimilarity) {}

    // 1) Показать, что текст -> вектор чисел. Выводим размерность и первые 5 значений.
    @GetMapping("/vector")
    public VectorInfo vector(@RequestParam String text) {
        float[] v = embeddingModel.embed(text);
        List<Float> preview = new ArrayList<>();
        for (int i = 0; i < Math.min(5, v.length); i++) {
            preview.add(v[i]);
        }
        return new VectorInfo(text, v.length, preview);
    }

    // 2) Косинусная близость двух текстов: 1.0 = одинаковый смысл, 0 = не связаны.
    @GetMapping("/similarity")
    public SimilarityResult similarity(@RequestParam String a, @RequestParam String b) {
        float[] va = embeddingModel.embed(a);
        float[] vb = embeddingModel.embed(b);
        return new SimilarityResult(a, b, cosine(va, vb));
    }

    private double cosine(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
