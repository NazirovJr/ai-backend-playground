package ai.backend.playground.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    // --- краевые случаи входа ---

    @Test
    void emptyTextReturnsEmptyList() {
        assertTrue(chunker.chunk("", 4, 1).isEmpty());
    }

    @Test
    void nullTextReturnsEmptyList() {
        assertTrue(chunker.chunk(null, 4, 1).isEmpty());
    }

    @Test
    void whitespaceOnlyTextReturnsEmptyList() {
        assertTrue(chunker.chunk("     ", 4, 1).isEmpty());
    }

    @Test
    void shorterThanChunkReturnsSingleChunk() {
        assertEquals(List.of("hi"), chunker.chunk("hi", 10, 2));
    }

    // --- нормализация пробелов ---

    @Test
    void collapsesMultipleSpaces() {
        assertEquals(List.of("a b c"), chunker.chunk("a    b\t\n c", 10, 2));
    }

    // --- валидация аргументов ---

    @Test
    void overlapGreaterOrEqualChunkSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> chunker.chunk("hello", 4, 4));
    }

    @Test
    void negativeOverlapThrows() {
        assertThrows(IllegalArgumentException.class, () -> chunker.chunk("hello", 4, -1));
    }

    @Test
    void nonPositiveChunkSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> chunker.chunk("hello", 0, 0));
    }

    // --- сам алгоритм разбиения ---

    @Test
    void splitWithOverlapHasNoTrailingDuplicate() {
        // ловит баг-хвост: раньше добавлялся лишний кусок "j"
        assertEquals(List.of("abcd", "defg", "ghij"), chunker.chunk("abcdefghij", 4, 1));
    }

    @Test
    void doesNotDropLastCharacter() {
        // ловит слишком жадный фикс: символ "i" не должен теряться
        assertEquals(List.of("abcd", "efgh", "i"), chunker.chunk("abcdefghi", 4, 0));
    }

    @Test
    void adjacentChunksOverlapByOverlapChars() {
        List<String> result = chunker.chunk("abcdefghij", 4, 1);
        // последние overlap символов куска == первые overlap символов следующего
        assertEquals("d", result.get(0).substring(3));
        assertEquals("d", result.get(1).substring(0, 1));
    }
}
