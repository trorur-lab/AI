from flask import Flask, render_template_string, request, jsonify
import pyodbc
import psycopg2

app = Flask(__name__)

HTML_TEMPLATE = '''
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Сравнение GUID между MS SQL и Postgres</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        h1 {
            color: #333;
            text-align: center;
        }
        .connection-forms {
            display: flex;
            gap: 20px;
            margin-bottom: 30px;
        }
        .connection-form {
            flex: 1;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .connection-form h3 {
            margin-top: 0;
            color: #444;
        }
        label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
            color: #555;
        }
        input[type="text"],
        input[type="password"] {
            width: 100%;
            padding: 8px;
            margin-bottom: 15px;
            border: 1px solid #ddd;
            border-radius: 4px;
            box-sizing: border-box;
        }
        button {
            background-color: #4CAF50;
            color: white;
            padding: 12px 24px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
            width: 100%;
        }
        button:hover {
            background-color: #45a049;
        }
        .results {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        th {
            background-color: #4CAF50;
            color: white;
        }
        tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        tr:hover {
            background-color: #f1f1f1;
        }
        .missing-mssql {
            background-color: #ffebee !important;
        }
        .missing-postgres {
            background-color: #e3f2fd !important;
        }
        .error {
            color: red;
            padding: 10px;
            background-color: #ffebee;
            border-radius: 4px;
            margin-bottom: 20px;
        }
        .summary {
            display: flex;
            gap: 20px;
            margin-bottom: 20px;
        }
        .summary-card {
            flex: 1;
            padding: 15px;
            border-radius: 8px;
            text-align: center;
        }
        .summary-card.total {
            background-color: #e8f5e9;
        }
        .summary-card.missing-mssql {
            background-color: #ffebee;
        }
        .summary-card.missing-postgres {
            background-color: #e3f2fd;
        }
        .summary-card h4 {
            margin: 0;
            font-size: 24px;
        }
        .summary-card p {
            margin: 5px 0 0 0;
            color: #666;
        }
    </style>
</head>
<body>
    <h1>Сравнение GUID между MS SQL и Postgres</h1>
    
    <div class="connection-forms">
        <div class="connection-form">
            <h3>🔗 Подключение к MS SQL Server</h3>
            <form id="mssql-form">
                <label for="mssql_server">Сервер:</label>
                <input type="text" id="mssql_server" name="mssql_server" placeholder="localhost\\SQLEXPRESS" required>
                
                <label for="mssql_database">База данных:</label>
                <input type="text" id="mssql_database" name="mssql_database" placeholder="DatabaseName" required>
                
                <label for="mssql_user">Пользователь:</label>
                <input type="text" id="mssql_user" name="mssql_user" placeholder="username" required>
                
                <label for="mssql_password">Пароль:</label>
                <input type="password" id="mssql_password" name="mssql_password" placeholder="password" required>
            </form>
        </div>
        
        <div class="connection-form">
            <h3>🐘 Подключение к PostgreSQL</h3>
            <form id="postgres-form">
                <label for="postgres_host">Хост:</label>
                <input type="text" id="postgres_host" name="postgres_host" placeholder="localhost" required>
                
                <label for="postgres_port">Порт:</label>
                <input type="text" id="postgres_port" name="postgres_port" placeholder="5432" value="5432" required>
                
                <label for="postgres_database">База данных:</label>
                <input type="text" id="postgres_database" name="postgres_database" placeholder="database_name" required>
                
                <label for="postgres_user">Пользователь:</label>
                <input type="text" id="postgres_user" name="postgres_user" placeholder="username" required>
                
                <label for="postgres_password">Пароль:</label>
                <input type="password" id="postgres_password" name="postgres_password" placeholder="password" required>
            </form>
        </div>
    </div>
    
    <button onclick="compareData()">🔍 Сравнить данные</button>
    
    <div id="results" class="results" style="display: none;">
        <div class="summary">
            <div class="summary-card total">
                <h4 id="total-count">0</h4>
                <p>Всего GUID в GuidList</p>
            </div>
            <div class="summary-card missing-mssql">
                <h4 id="missing-mssql-count">0</h4>
                <p>Отсутствует в MS SQL</p>
            </div>
            <div class="summary-card missing-postgres">
                <h4 id="missing-postgres-count">0</h4>
                <p>Отсутствует в PostgreSQL</p>
            </div>
        </div>
        
        <h2>Результаты сравнения</h2>
        <table id="results-table">
            <thead>
                <tr>
                    <th>GUID</th>
                    <th>ID документа (MS SQL)</th>
                    <th>Наименование (MS SQL)</th>
                    <th>ID документа (Postgres)</th>
                    <th>Наименование (Postgres)</th>
                    <th>Статус</th>
                </tr>
            </thead>
            <tbody id="results-body">
            </tbody>
        </table>
    </div>
    
    <div id="error" class="error" style="display: none;"></div>

    <script>
        async function compareData() {
            const mssqlServer = document.getElementById('mssql_server').value;
            const mssqlDatabase = document.getElementById('mssql_database').value;
            const mssqlUser = document.getElementById('mssql_user').value;
            const mssqlPassword = document.getElementById('mssql_password').value;
            
            const postgresHost = document.getElementById('postgres_host').value;
            const postgresPort = document.getElementById('postgres_port').value;
            const postgresDatabase = document.getElementById('postgres_database').value;
            const postgresUser = document.getElementById('postgres_user').value;
            const postgresPassword = document.getElementById('postgres_password').value;
            
            document.getElementById('error').style.display = 'none';
            document.getElementById('results').style.display = 'none';
            
            try {
                const response = await fetch('/compare', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        mssql: {
                            server: mssqlServer,
                            database: mssqlDatabase,
                            user: mssqlUser,
                            password: mssqlPassword
                        },
                        postgres: {
                            host: postgresHost,
                            port: parseInt(postgresPort),
                            database: postgresDatabase,
                            user: postgresUser,
                            password: postgresPassword
                        }
                    })
                });
                
                const data = await response.json();
                
                if (!response.ok) {
                    throw new Error(data.error || 'Ошибка при выполнении запроса');
                }
                
                document.getElementById('total-count').textContent = data.total;
                document.getElementById('missing-mssql-count').textContent = data.missing_in_mssql.length;
                document.getElementById('missing-postgres-count').textContent = data.missing_in_postgres.length;
                
                const tbody = document.getElementById('results-body');
                tbody.innerHTML = '';
                
                // Объединяем все записи
                const allRecords = new Map();
                
                // Добавляем записи из MS SQL
                data.mssql_records.forEach(record => {
                    allRecords.set(record.guid.toLowerCase(), {
                        guid: record.guid,
                        mssql_doc_id: record.doc_id,
                        mssql_name: record.name,
                        postgres_doc_id: null,
                        postgres_name: null
                    });
                });
                
                // Добавляем/обновляем записи из Postgres
                data.postgres_records.forEach(record => {
                    const key = record.guid.toLowerCase();
                    if (allRecords.has(key)) {
                        const existing = allRecords.get(key);
                        existing.postgres_doc_id = record.doc_id;
                        existing.postgres_name = record.name;
                    } else {
                        allRecords.set(key, {
                            guid: record.guid,
                            mssql_doc_id: null,
                            mssql_name: null,
                            postgres_doc_id: record.doc_id,
                            postgres_name: record.name
                        });
                    }
                });
                
                // Сортируем и отображаем
                const sortedRecords = Array.from(allRecords.values()).sort((a, b) => 
                    a.guid.localeCompare(b.guid)
                );
                
                sortedRecords.forEach(record => {
                    const tr = document.createElement('tr');
                    
                    let status = '✓ В обеих базах';
                    if (!record.mssql_doc_id && !record.postgres_doc_id) {
                        status = '✗ Отсутствует в обеих';
                        tr.className = 'missing-mssql missing-postgres';
                    } else if (!record.mssql_doc_id) {
                        status = '✗ Отсутствует в MS SQL';
                        tr.className = 'missing-mssql';
                    } else if (!record.postgres_doc_id) {
                        status = '✗ Отсутствует в PostgreSQL';
                        tr.className = 'missing-postgres';
                    }
                    
                    tr.innerHTML = `
                        <td>${record.guid}</td>
                        <td>${record.mssql_doc_id !== null ? record.mssql_doc_id : '<span style="color: red;">отсутствует</span>'}</td>
                        <td>${record.mssql_name !== null ? record.mssql_name : '<span style="color: red;">-</span>'}</td>
                        <td>${record.postgres_doc_id !== null ? record.postgres_doc_id : '<span style="color: red;">отсутствует</span>'}</td>
                        <td>${record.postgres_name !== null ? record.postgres_name : '<span style="color: red;">-</span>'}</td>
                        <td>${status}</td>
                    `;
                    tbody.appendChild(tr);
                });
                
                document.getElementById('results').style.display = 'block';
                
            } catch (error) {
                document.getElementById('error').textContent = 'Ошибка: ' + error.message;
                document.getElementById('error').style.display = 'block';
            }
        }
    </script>
</body>
</html>
'''

def get_mssql_connection(server, database, user, password):
    """Подключение к MS SQL Server"""
    connection_string = f'DRIVER={{ODBC Driver 17 for SQL Server}};SERVER={server};DATABASE={database};UID={user};PWD={password}'
    return pyodbc.connect(connection_string)

def get_postgres_connection(host, port, database, user, password):
    """Подключение к PostgreSQL"""
    return psycopg2.connect(
        host=host,
        port=port,
        database=database,
        user=user,
        password=password
    )

def fetch_mssql_data(conn):
    """Получение данных из MS SQL Server"""
    query = """
    SELECT 
        si.GlobalID AS GuidValue,
        si.LocalID AS DocId, 
        mb.NameAn AS Name
    FROM SBGlobalIDs si
    JOIN GuidList gl ON gl.GuidValue = si.GlobalID
    JOIN MBAnalit mb ON si.LocalID = mb.Analit
    """
    
    cursor = conn.cursor()
    cursor.execute(query)
    
    results = []
    for row in cursor.fetchall():
        results.append({
            'guid': str(row.GuidValue) if row.GuidValue else None,
            'doc_id': row.DocId,
            'name': row.Name
        })
    
    return results

def fetch_postgres_data(conn):
    """Получение данных из PostgreSQL"""
    query = """
    SELECT 
        sce.extentityid AS guid_value,
        doc.id AS doc_id, 
        doc."name" AS name
    FROM sungero_commons_extentitylinks sce
    JOIN guid_list gl ON gl.guid_value::citext = sce.extentityid
    JOIN sungero_content_edoc doc ON doc.id = sce.entityid
    """
    
    cursor = conn.cursor()
    cursor.execute(query)
    
    results = []
    for row in cursor.fetchall():
        results.append({
            'guid': str(row[0]) if row[0] else None,
            'doc_id': row[1],
            'name': row[2]
        })
    
    return results

@app.route('/')
def index():
    return render_template_string(HTML_TEMPLATE)

@app.route('/compare', methods=['POST'])
def compare():
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'Нет данных для обработки'}), 400
        
        mssql_config = data.get('mssql', {})
        postgres_config = data.get('postgres', {})
        
        # Валидация конфигурации MS SQL
        required_mssql = ['server', 'database', 'user', 'password']
        for field in required_mssql:
            if not mssql_config.get(field):
                return jsonify({'error': f'Не указано поле MS SQL: {field}'}), 400
        
        # Валидация конфигурации PostgreSQL
        required_postgres = ['host', 'port', 'database', 'user', 'password']
        for field in required_postgres:
            if not postgres_config.get(field):
                return jsonify({'error': f'Не указано поле PostgreSQL: {field}'}), 400
        
        # Подключение к MS SQL и получение данных
        mssql_conn = None
        postgres_conn = None
        
        try:
            mssql_conn = get_mssql_connection(
                mssql_config['server'],
                mssql_config['database'],
                mssql_config['user'],
                mssql_config['password']
            )
            mssql_records = fetch_mssql_data(mssql_conn)
        except Exception as e:
            return jsonify({'error': f'Ошибка подключения к MS SQL: {str(e)}'}), 500
        finally:
            if mssql_conn:
                mssql_conn.close()
        
        try:
            postgres_conn = get_postgres_connection(
                postgres_config['host'],
                postgres_config['port'],
                postgres_config['database'],
                postgres_config['user'],
                postgres_config['password']
            )
            postgres_records = fetch_postgres_data(postgres_conn)
        except Exception as e:
            return jsonify({'error': f'Ошибка подключения к PostgreSQL: {str(e)}'}), 500
        finally:
            if postgres_conn:
                postgres_conn.close()
        
        # Создаем словари для быстрого поиска по GUID
        mssql_dict = {r['guid'].lower(): r for r in mssql_records if r['guid']}
        postgres_dict = {r['guid'].lower(): r for r in postgres_records if r['guid']}
        
        # Находим GUID, которые есть в GuidList но отсутствуют в MS SQL
        missing_in_mssql = []
        missing_in_postgres = []
        
        # Получаем все GUID из GuidList (через объединение данных)
        all_guids = set(mssql_dict.keys()) | set(postgres_dict.keys())
        
        for guid in all_guids:
            if guid not in mssql_dict:
                missing_in_mssql.append(guid)
            if guid not in postgres_dict:
                missing_in_postgres.append(guid)
        
        return jsonify({
            'total': len(all_guids),
            'mssql_records': mssql_records,
            'postgres_records': postgres_records,
            'missing_in_mssql': missing_in_mssql,
            'missing_in_postgres': missing_in_postgres
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
