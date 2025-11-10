# License Kafka Service

Микросервис для обработки событий о лицензиях ПО, получаемых из Kafka или через REST API.  
Использует **паттерн Outbox** — все входящие сообщения сначала сохраняются в таблицу `outbox_event`, а затем асинхронно обрабатываются планировщиком, который записывает данные о лицензиях в таблицу `software_license`.

---
## Описание работы

1. **KafkaListener** слушает топик `software-licenses`  
   • сохраняет сообщение в `outbox_event`

2. **OutboxScheduler** каждые 5 секунд:  
   • читает необработанные события,  
   • преобразует их в `SoftwareLicenseEntity`,  
   • сохраняет/обновляет данные о лицензии,  
   • помечает событие как обработанное.

3. **Ежедневная очистка** (`03:00`)  
   • удаляет старые обработанные события старше `outbox.retention-days` (7 дней).

---

## Запуск проекта

### Сборка jar-файла
```bash
mvn clean package -DskipTests
```
### Запуск через Docker Compose
```bash
docker-compose up --build
```
### REST API

**POST** /api/licenses

Отправляет JSON с лицензией в outbox_event

**Пример запроса:**
```bash
curl -X POST http://localhost:8080/api/licenses \
  -H "Content-Type: application/json" \
  -d '{
        "licenseId": "LIC-100",
        "softwareName": "Figma",
        "owner": "Victoria",
        "expiresAt": "2026-05-01"
      }'
```
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