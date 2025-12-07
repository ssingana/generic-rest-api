
Dynamic Query Full Stack - QuickStart
====================================

Backend:
- Java 11, Spring Boot 2.7.12, H2 in-memory DB
- GenericQueryService with:
    - Dynamic projections
    - Filters: equals, _like, _between, _inSubquery
    - Multi-column sorting (sorts[])
    - Distinct support
    - Join support via dot notation: department.name
    - Excel export

Frontend:
- Served from src/main/resources/static
- Open http://localhost:8080/index.html after starting backend
- UI allows selecting entity, fields, filters, distinct, pagination, and export.

Run:
1) mvn clean spring-boot:run
2) Open: http://localhost:8080/index.html
3) Use sample payload dropdown or manual filters.

H2 Console:
- http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:testdb
- User: sa
- Password: (blank)
