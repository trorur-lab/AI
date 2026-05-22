# Программа для сравнения GUID между MS SQL Server и PostgreSQL

## Описание

Веб-приложение на Flask для сравнения наличия записей по GUID между двумя базами данных:
- MS SQL Server
- PostgreSQL

Связь между сущностями осуществляется через таблицу `GuidList` (MS SQL) / `guid_list` (PostgreSQL).

## Требования

- Python 3.8+
- Библиотеки из requirements.txt
- ODBC драйвер для MS SQL Server (для Linux: msodbcsql17 или msodbcsql18)

## Установка

### 1. Установка системных зависимостей (для Linux)

```bash
# Установка ODBC менеджера
apt-get install -y unixodbc unixodbc-dev

# Установка драйвера MS SQL Server (Ubuntu/Debian)
curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add -
curl https://packages.microsoft.com/config/ubuntu/$(lsb_release -rs)/prod.list > /etc/apt/sources.list.d/mssql-release.list
apt-get update
ACCEPT_EULA=Y apt-get install -y msodbcsql17
```

### 2. Установка Python зависимостей

```bash
pip install -r requirements.txt
```

## Запуск

```bash
python app.py
```

Приложение будет доступно по адресу: http://localhost:5000

## Использование

1. Откройте веб-браузер и перейдите на http://localhost:5000
2. Заполните параметры подключения к MS SQL Server:
   - Сервер (например, localhost\\SQLEXPRESS)
   - База данных
   - Пользователь
   - Пароль

3. Заполните параметры подключения к PostgreSQL:
   - Хост (например, localhost)
   - Порт (по умолчанию 5432)
   - База данных
   - Пользователь
   - Пароль

4. Нажмите кнопку "Сравнить данные"

## Результат

Программа выведет таблицу со следующими колонками:
- **GUID** - значение GUID из таблицы GuidList
- **ID документа (MS SQL)** - LocalID из таблицы SBGlobalIDs
- **Наименование (MS SQL)** - NameAn из таблицы MBAnalit
- **ID документа (Postgres)** - id из таблицы sungero_content_edoc
- **Наименование (Postgres)** - name из таблицы sungero_content_edoc
- **Статус** - показывает, в какой базе присутствует запись

Записи подсвечиваются цветом:
- 🔴 Красный фон - отсутствует в MS SQL
- 🔵 Синий фон - отсутствует в PostgreSQL

Также отображается сводка:
- Всего GUID в GuidList
- Количество записей, отсутствующих в MS SQL
- Количество записей, отсутствующих в PostgreSQL

## SQL запросы

### MS SQL Server
```sql
SELECT 
    si.GlobalID AS GuidValue,
    si.LocalID AS DocId, 
    mb.NameAn AS Name
FROM SBGlobalIDs si
JOIN GuidList gl ON gl.GuidValue = si.GlobalID
JOIN MBAnalit mb ON si.LocalID = mb.Analit
```

### PostgreSQL
```sql
SELECT 
    sce.extentityid AS guid_value,
    doc.id AS doc_id, 
    doc."name" AS name
FROM sungero_commons_extentitylinks sce
JOIN guid_list gl ON gl.guid_value::citext = sce.extentityid
JOIN sungero_content_edoc doc ON doc.id = sce.entityid
```

## Структура проекта

```
/workspace
├── app.py              # Основное приложение Flask
├── requirements.txt    # Зависимости Python
└── README.md          # Эта инструкция
```

## Примечания

- Приложение не хранит учетные данные - они используются только для текущего сеанса
- Для работы требуется наличие таблиц:
  - В MS SQL: `GuidList`, `SBGlobalIDs`, `MBAnalit`
  - В PostgreSQL: `guid_list`, `sungero_commons_extentitylinks`, `sungero_content_edoc`
- Сравнение выполняется регистронезависимо (GUID приводятся к нижнему регистру)
