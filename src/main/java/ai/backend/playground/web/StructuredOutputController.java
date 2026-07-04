package ai.backend.playground.web;

import ai.backend.playground.model.Employee;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Урок 1.3 — Structured Output.
 * Модель возвращает не текст, а типизированный Java-объект (record),
 * который можно безопасно использовать в коде.
 */
@RestController
@RequestMapping("/structured")
public class StructuredOutputController {

    private final ChatClient chatClient;

    public StructuredOutputController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // enum'ы делают схему ещё строже: модель обязана выбрать из фиксированного набора
    public enum Category { BILLING, BUG, FEATURE, OTHER }
    public enum Priority { LOW, MEDIUM, HIGH }

    // record = целевая структура ответа. Spring AI сам построит по нему JSON-схему.
    public record TicketClassification(Category category, Priority priority, String reason) {}

    public record ActionItem(String task, String owner) {}

    // 1) Один объект: классификация обращения в типизированный record.
    @GetMapping("/classify")
    public TicketClassification classify(@RequestParam String message) {
        return chatClient.prompt()
                .system("""
                        Ты классифицируешь обращение в поддержку.
                        Определи категорию, приоритет и краткую причину (reason) на русском.
                        """)
                .user(u -> u.text("Обращение: {msg}").param("msg", message))
                .options(ChatOptions.builder().temperature(0.0))
                .call()
                .entity(TicketClassification.class);   // <-- вместо .content()
    }

    // 2) Список объектов: извлечение задач из свободного текста.
    //    Для дженериков нужен ParameterizedTypeReference.
    @GetMapping("/extract")
    public List<ActionItem> extract(@RequestParam String text) {
        return chatClient.prompt()
                .user(u -> u.text("""
                        Извлеки список задач и ответственных из текста.
                        Если ответственный не указан — оставь "не назначен".
                        Текст: {text}
                        """).param("text", text))
                .options(ChatOptions.builder().temperature(0.0))
                .call()
                .entity(new ParameterizedTypeReference<List<ActionItem>>() {});
    }

    @GetMapping("/roles")
    public List<Employee> extractRolesAndTask(@RequestParam String text) {
        return chatClient.prompt()
                .system("""
                        На вход к тебе попадет текст в нем будет содержатся имена и задачи которые нужно сделать исходя из этого возвращай имя сотрудника и список задач который он должен выполнить
                        Если у задачи нету исполнителя пиши : "Нету исполнителя".А если у сотрудника нету задачи просто возвращай сотрудника с пистим значением
                        """)
                .user(text)
                .call()
                .entity(new ParameterizedTypeReference<List<Employee>>() {});
    }
}
