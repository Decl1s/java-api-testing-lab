# Результат проверки — 5 октября 2026

Среда: OpenJDK 17.0.20.1, Maven 3.8.7, Linux; локальный HTTP-сервер на 127.0.0.1.

- Обычный запуск: 32 tests, 0 failures, 0 errors, 0 skipped.
- demoBug=true: 32 tests, 2 failures, 0 errors, 0 skipped.
- Падения: TC-03, значения 4097 и 2147483647; expected 422, actual 201.
- SQL-кейс: все шесть запросов дали результаты из sql/expected.md.

Это результаты локального запуска. Статус GitHub Actions фиксируется отдельно после публикации.
