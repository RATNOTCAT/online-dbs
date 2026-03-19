# Sterling Bank Portal

Online banking portal with a React frontend and a Spring Boot backend.

## Stack

- React + Vite + TypeScript
- Java 17 + Spring Boot 3
- SQLite
- JWT authentication

## Run locally

### Frontend

```bash
npm install
npm run dev
```

### Backend

```bash
cd backend-java
mvn spring-boot:run
```

On this Windows setup, the easiest option is:

```powershell
.\run-backend.ps1
```

Or:

```bat
run-backend.bat
```

To stop the backend process on port `5000`:

```powershell
.\stop-backend.ps1
```

The frontend expects the API at `http://localhost:5000/api`.

## Backends in this repo

- `backend-java/` is the primary backend.
- `backend/` is the legacy Flask backend kept for reference during migration.

## More docs

- [PROJECT_README.md](PROJECT_README.md)
- [backend-java/README.md](backend-java/README.md)
