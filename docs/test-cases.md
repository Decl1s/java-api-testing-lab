# Матрица проверок

| ID | Сценарий | Ожидаемый результат | Автотест |
|---|---|---|---|
| TC-01 | Создать sample.pdf, 12 KB; прочитать Location | 201 → 200, поля совпадают | createAndRead |
| TC-02 | Размер 1, 2, 4095, 4096 | 201 | validSize |
| TC-03 | Размер -1, 0, 4097, 2147483647 | 422, validation_error | invalidSize |
| TC-04 | Пропуски, пустое имя, null, неправильные типы | 422 | invalidFields |
| TC-05 | Пустое тело, сломанный JSON, null, массив | 400 | invalidJson |
| TC-06 | Нет ключа, неправильный/пустой ключ | 401 | unauthorized |
| TC-07 | text/plain | 415 | contentType |
| TC-08 | Несуществующий id | 404 | unknownJob |
| TC-09 | Отмена → GET → повторная отмена | 200 CANCELLED → 200 CANCELLED → 409 | cancel |
| TC-10 | Создать два задания, отменить первое | Разные id; второе QUEUED | independentJobs |
| TC-11 | Имя длиной 255/256 символов | 201/422 | filenameLength |
| TC-12 | DELETE существующего задания | 405 | method |
| TC-13 | Тело 8193 байта | 413 | oversizedBody |
| TC-14 | Создать и прочитать отчёт.pdf / Заявка №42.txt | 201 → 200, имя сохранено без искажений; charset=utf-8 | preservesCyrillicFilename |
| TC-15 | JSON с charset=utf-8; регистр Application/JSON | 201 → 200, поля сохранены | acceptsJsonWithCharset |
| TC-16 | Корректный JSON без Content-Type | 415 unsupported_media_type; задание не создано; следующий корректный POST работает | rejectsMissingContentType |
| TC-17 | GET и POST: /missing, /jobs/, нечисловой/отрицательный/переполненный id, лишний сегмент после cancel | 404 not_found в JSON; существующее задание не меняется | rejectsUnknownRoutesWithoutChangingJob |
| TC-18 | Корректный JSON с кириллицей, дополненный пробелами до 8191/8192/8193 байт UTF-8 | 201/201/413 body_too_large; при отказе задание не создаётся | enforcesBodyLimitInBytes |

Известные непокрытые варианты: переполнение long в sizeKb. Они не включены в заявление о покрытии.
