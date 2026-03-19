# Spring Boot Backend

This folder contains a Java + Spring Boot replacement for the old Flask backend.

## Stack

- Java 17
- Spring Boot 3
- Spring Security with JWT bearer auth
- Spring Data JPA
- SQLite database

## Run

```bash
cd backend-java
mvn spring-boot:run
```

The API runs on `http://localhost:5000/api`.

## Notes

- The frontend does not need API path changes.
- By default this app points to `../backend/instance/banking.db` so it can reuse your existing SQLite file.
- The Flask backend can stay in the repo during migration, but do not run both backends on port `5000` at the same time.
