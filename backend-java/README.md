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
- The Spring Boot backend stores its SQLite data in `backend-java/data/banking.db`.
- To migrate from the old Flask backend, copy `backend/instance/banking.db` into `backend-java/data/banking.db`.
- To force a specific SQLite file, start the app with `--app.db.path=...` or set `BANK_DB_PATH`.
- If you still have a legacy backend, do not run both backends on port `5000` at the same time.
