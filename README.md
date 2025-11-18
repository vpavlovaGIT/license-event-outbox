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
 
### Проверка работы паттерна Outbox
1. Проверить таблицу outbox_event:
```sql
SELECT * FROM outbox_event ORDER BY id DESC;
```
Новая запись должна содержать **processed = false**
2. Подожди 5 секунд (scheduler срабатывает каждые 5 секунд)
3. Проверить  таблицу лицензий:
```sql
SELECT * FROM software_license;
```
Появится запись с `license_id = 'LIC-100'`

4. Проверить, что событие обработано:
```sql
SELECT processed, processed_at FROM outbox_event ORDER BY id DESC;
```
Поле `processed`станет true, `processed_at` заполнится временем обработки

### Проверка в Kafka UI:

1. Перейти по ссылке `http://localhost:8081`
2. Открыть топик `software-licenses`
3. Создать новое сообщение прямо в UI:
   ```json
   {
     "licenseId": "LIC-500",
     "softwareName": "JetBrains IntelliJ IDEA",
     "owner": "Victoria",
     "expiresAt": "2027-02-15"
   }
4. Через 5 секунд проверить таблицу software_license — новая лицензия должна появиться