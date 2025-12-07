QUICKSTART - Java 11 + H2 version
================================

Prerequisites
- Java 11 installed
- Maven installed

Run the application
1. mvn clean package
2. mvn spring-boot:run
   or run the generated jar: java -jar target/dynamic-query-java11-h2-0.0.1-SNAPSHOT.jar

H2 Console
- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:testdb
- Username: sa
- Password: (blank)

API Endpoints
1) POST /dynamic/fetch
   - Fetch paginated results with dynamic projections and filters.
   - Example payloads (use Postman):

A) Simple LIKE + projection + pagination
POST http://localhost:8080/dynamic/fetch
Content-Type: application/json
Body:
{
  "entity": "Employee",
  "fields": ["id","name","salary","joiningDate","department.name"],
  "filters": {
    "name_like": "John"
  },
  "page": 0,
  "size": 10,
  "sort": "name,asc",
  "export": false
}

B) BETWEEN on salary
{
  "entity": "Employee",
  "fields": ["id","name","salary"],
  "filters": {
    "salary_between": [30000, 80000]
  },
  "page": 0,
  "size": 10
}

C) Department listing
{
  "entity": "Department",
  "fields": ["id","name"],
  "filters": {},
  "page": 0,
  "size": 10
}

2) Export to Excel (use export=true in payload)
{
  "entity": "Employee",
  "fields": ["id","name","salary","department.name"],
  "filters": {
    "salary_between": [40000, 90000]
  },
  "export": true
}
Response: { "file": "/tmp/..._export.xlsx", "message": "Excel export successful" }

3) Download exported file
GET http://localhost:8080/dynamic/download?filePath=/path/to/file.xlsx

Notes
- 500 Employee rows are auto-loaded at startup.
- For production, do not expose raw file paths. Implement secure file storage and cleanup.
