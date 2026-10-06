# Контракт учебного API

Все запросы требуют `X-Api-Key: demo-token`. Неверный или отсутствующий ключ: 401 и `error: unauthorized`.

POST /jobs: Content-Type application/json, тело `{ "filename": "sample.pdf", "sizeKb": 12 }`.

Требования: filename — непустая строка, не состоящая только из пробелов, длина 1–255 Java char; sizeKb — целое число 1–4096 включительно. Ошибка полей: 422, validation_error. Синтаксическая ошибка JSON, пустое тело или корень не объект: 400, invalid_json. Тип содержимого не JSON: 415, unsupported_media_type. Тело свыше 8192 байт: 413, body_too_large. Проверка ключа выполняется раньше проверки тела.

Успех: 201, JSON с id, filename, sizeKb, status=QUEUED; заголовок Location: /jobs/{id}. Каждый успешный запрос создаёт отдельный id.

GET /jobs/{id}: 200 и сохранённый объект; неизвестный id: 404, not_found.

POST /jobs/{id}/cancel: 200, статус CANCELLED. Повторная отмена: 409, already_cancelled. GET после отмены возвращает CANCELLED. Отмена одного задания не меняет другие задания.

Неподдерживаемый метод для существующего задания: 405, method_not_allowed. Неизвестный маршрут: 404. Все ответы JSON, Content-Type application/json; charset=utf-8.

Статусы сохраняются в памяти до остановки стенда. Самостоятельного перехода в DONE и фактической обработки файлов нет.
