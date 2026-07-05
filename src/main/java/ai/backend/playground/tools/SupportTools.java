package ai.backend.playground.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Урок 1.4 — инструменты (Tools), которые модель может вызывать сама.
 * Метод, помеченный @Tool, становится доступен модели: она сама решает,
 * когда его вызвать, Spring AI выполняет метод и отдаёт результат обратно модели.
 */
@Component
public class SupportTools {

    private final RestClient restClient;

    public SupportTools(RestClient restClient) {
        this.restClient = restClient;
    }

    // Модель НЕ знает текущее время — чтобы ответить, она обязана вызвать этот метод.
    @Tool(description = "Возвращает текущую дату и время сервера")
    public String currentDateTime() {
        return LocalDateTime.now().toString();
    }

    // Мок "базы заказов" — имитируем поход в БД/внешний сервис.
    private static final Map<String, String> ORDERS = Map.of(
            "123", "Отправлен, доставка ожидается 5 июля",
            "456", "Оплачен, собирается на складе",
            "789", "Отменён по запросу клиента"
    );

    @Tool(description = "Возвращает статус заказа по его номеру")
    public String getOrderStatus(
            @ToolParam(description = "Номер заказа, например 123") String orderId) {
        return ORDERS.getOrDefault(orderId, "Заказ с таким номером не найден");
    }

    @Tool(description = "Возвращает текущую погоду")
    public String getWeather(@ToolParam(description = "Название города, например Москва") String city) {
                return restClient.get()
                        // format=3 -> компактная строка "Город: ☀️ +18°C" (j1 давал огромный JSON,
                        // от которого маленькая локальная модель "захлёбывалась")
                        .uri("https://wttr.in/{city}?format=3", city)
                        .retrieve()
                        .body(String.class);
    }

}
