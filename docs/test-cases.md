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

Известные непокрытые варианты: отсутствующий Content-Type, JSON с charset, UTF-8 имя, переполнение long в sizeKb, неизвестные маршруты, край тела ровно 8192 байта. Они не включены в заявление о покрытии.
