# License Kafka Service

Сервис принимает сообщения из Kafka, сохраняет их в таблицу outbox_event и поддерживает работу с лицензиями ПО.

---

## Запуск проекта

### Сборка jar-файла
```bash
mvn clean package -DskipTests
```
После сборки появится:

```bash
target/license-event-outbox-1.0.0.jar
```

### Запуск через Docker Compose
```bash
docker-compose up --build
```
| Сервис          | Порт        | Описание               |
|-----------------|-------------|-------------------------|
| PostgreSQL      | 5432        | база данных             |
| Kafka           | 9092/29092  | брокер Kafka            |
| Zookeeper       | 2181        | сервис координатора     |
| License Service | 8080        | наше приложение         |
| Kafka UI        | 8081        | веб-интерфейс Kafka     |
 
### Проверка работы паттерна Outbox через Docker
1. Предварительная очистка таблицы
```bash
docker exec -it postgres psql -U postgres -d licenses -c "DELETE FROM outbox_event; DELETE FROM software_license;"
```
2. Публикация тестового сообщения в Kafka (через Kafka UI на http://localhost:8081)
   • Topics > software-licenses > Produce Message.
   • Ключ: пустой.
   • Значение:
```json
   {
     "licenseId": "LIC-100",
     "softwareName": "JetBrains IntelliJ IDEA",
     "owner": "Victoria",
     "expiresAt": "2027-02-15"
   }
```
2. Проверить таблицу outbox_event:
```bash
docker exec -it postgres psql -U postgres -d licenses -c "SELECT * FROM outbox_event ORDER BY id DESC LIMIT 1;"
```
Новая запись должна содержать **processed = false**
2. Подождать 5 секунд (scheduler срабатывает каждые 5 секунд fixedDelayString = "5000")
3. Проверить  таблицу лицензий:
```bash
docker exec -it postgres psql -U postgres -d licenses -c "SELECT * FROM software_license WHERE license_id = 'LIC-100';"
```
Появится запись с `license_id = 'LIC-100'`
4. Проверить, что событие обработано:
```bash
docker exec -it postgres psql -U postgres -d licenses -c "SELECT processed, processed_at FROM outbox_event ORDER BY id DESC LIMIT 1;"
```
Поле `processed`станет true, `processed_at` заполнится временем обработки

### Интеграционный тест LicenseKafkaIntegrationTest
Интеграционный тест **LicenseKafkaIntegrationTest** проверяет, что сообщение,
отправленное в Kafka, корректно сохраняется в таблицу **outbox_event** (паттерн Outbox).
#### Описание работы теста
1. Подготовка **(BeforeAll)**: Запускает контейнеры PostgreSQL (с БД licenses) и Kafka
2. Настройка свойств (**@DynamicPropertySource)**: Динамически конфигурирует Spring Boot:

   • ```spring.kafka.bootstrap-servers ```: Адрес Kafka из контейнера.

   • ```spring.kafka.consumer.group-id```: Группа потребителей для теста.

   • ```spring.datasource.*```: Параметры подключения к PostgreSQL.
3. **Тестовый сценарий** (testKafkaMessageIsSavedToOutbox):

   **GIVEN:** Создаёт тестовое JSON-сообщение о лицензии ПО (с licenseId: "LIC-TST").

   **WHEN:** Отправляет сообщение в топик software-licenses через KafkaTemplate<String, String>. Ждёт 2 секунды для асинхронной обработки (сообщение должно быть поймано слушателем LicenseKafkaListener и сохранено в репозитории).

   **THEN:**
   Получает все записи из OutboxEventRepository.
   Проверяет, что список событий не пустой (assertThat(events).isNotEmpty()).
   Проверяет, что payload первого события содержит LIC-TST (assertThat(events.get(0).getPayload()).contains("LIC-TST")).
4. Очистка (**AfterAll**): Останоывка контейнеров