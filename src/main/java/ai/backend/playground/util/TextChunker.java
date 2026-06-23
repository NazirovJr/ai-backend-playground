package ai.backend.playground.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Режет длинный текст на куски заданного размера (в символах) с перекрытием.
 * Перед разбиением нормализует пробелы: схлопывает любые пробельные
 * последовательности в один пробел и обрезает края.
 */
public class TextChunker {

    /**
     * @param text      исходный текст (может быть null)
     * @param chunkSize максимальный размер куска в символах (> 0)
     * @param overlap   перекрытие соседних кусков в символах (0 <= overlap < chunkSize)
     * @return список кусков; пустой список для null/пустого текста
     */
    public List<String> chunk(String text, int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0, got " + chunkSize);
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException(
                "overlap must satisfy 0 <= overlap < chunkSize, got " + overlap);
        }

        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }

        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return chunks;
        }
        if (normalized.length() <= chunkSize) {
            chunks.add(normalized);
            return chunks;
        }

        int step = chunkSize - overlap;
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + chunkSize, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end == normalized.length()) {
                break; // дошли до конца текста — дальше будут только дубликаты хвоста
            }
        }
        return chunks;
    }
}
