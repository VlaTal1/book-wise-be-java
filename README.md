# Book Wise — Java backend

Main REST API of **Book Wise**, a mobile app that helps primary-school children practise reading
and helps teachers track their progress. This service owns all persistent data: users and roles,
books, classrooms, reading progress, comprehension tests, classroom reading assignments,
reading-speed attempts and statistics.

It is one of three repositories:

| Repository | Role |
|---|---|
| `book-wise-mobile` | React Native / Expo app for students and teachers |
| `book-wise-be-java` (this one) | REST API, database, file storage, authorization |
| `book-wise-be-python` | Speech recognition for reading-speed sessions, AI test generation, stress check |

## Tech stack

- Java 21, Spring Boot 3.4 (Web, Security, Data JPA, Actuator)
- PostgreSQL 17 + Liquibase migrations
- MinIO (S3-compatible storage) for book files and reading-session audio
- RabbitMQ for asynchronous test generation (handled by the Python service)
- Supabase Auth — the API validates Supabase-issued JWTs (JWKS from `SUPABASE_URL`)
- Lombok, Gradle

## How it fits together

```
mobile ──REST + Supabase JWT──▶ Java (:8080) ──JPA──▶ PostgreSQL
                                   │  ├──────────────▶ MinIO (books, audio)
                                   │  ├─ RabbitMQ ───▶ Python: test_generation_request
                                   │  │ ◀──────────── Python: test_generation_response
                                   │  └─ HTTP ───────▶ Python /internal/book-paragraphs
                                   ▲
       Python ──/internal/** + X-API key──┘  (saves reading-speed results, stress-check progress)
```

- `/api/**` — called by the mobile app, requires a Supabase JWT (`Authorization: Bearer …`).
- `/internal/**` — called only by the Python service, authorized with the shared `INTERNAL_API_KEY`.

## Getting started

### Prerequisites

- JDK 21
- Docker (for PostgreSQL and MinIO)
- RabbitMQ — started from `book-wise-be-python/docker-compose.yaml`
- A Supabase project (for authentication)

### 1. Start infrastructure

```bash
docker compose up -d            # PostgreSQL (:5432) and MinIO (API :9002, console :9001)
```

Create the bucket named in `MINIO_BUCKET_NAME` in the MinIO console (http://localhost:9001,
`minioadmin` / `minioadmin`) if it does not exist yet.

### 2. Configure environment

Create a `.env` file in the repository root (it is read on startup by `dotenv-java`):

```dotenv
SUPABASE_URL=https://<project>.supabase.co

# Shared secret for service-to-service calls; must equal JAVA_INTERNAL_API_KEY in book-wise-be-python
INTERNAL_API_KEY=<random-string>

MINIO_ENDPOINT=http://localhost:9002
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=books

POSTGRES_URL=jdbc:postgresql://localhost:5432/reader-ai

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VIRTUAL_HOST=/
```

Optional: `PYTHON_BASE_URL` (default `http://localhost:8001`) — address of the Python service.

> Database credentials and RabbitMQ connection settings are currently set in
> `src/main/resources/application.yml`; adjust them there if your setup differs from the
> Docker Compose defaults.

### 3. Run

```bash
./gradlew bootRun
```

The API listens on `http://localhost:8080`. Liquibase applies all migrations on startup, and
Hibernate runs with `ddl-auto: validate`, so the schema is always defined by the changelogs.

### Tests and build

```bash
./gradlew test
./gradlew bootJar               # build/libs/*.jar
```

## API overview

| Area | Base path | Purpose |
|---|---|---|
| Participants | `/api/participant` | Current user profile and role (teacher / student), student list |
| Books | `/api/books` | Upload, list, download and delete books; built-in book catalog |
| Progress | `/api/progress` | Reading position of a participant in a book |
| Reading sessions | `/api/readingSession` | Log time spent reading |
| Tests | `/api/test` | Comprehension tests for books, test passing |
| Classrooms | `/api/classrooms` | Classrooms, join codes, local students, assigned books and tests |
| Classroom readings | `/api/classrooms/{id}/readings`, `/api/classroom-readings` | Teacher-assigned reading-speed fragments and their results |
| Reading-speed attempts | `/api/reading-speed-attempts` | History of reading-speed attempts, audio playback, quiz answers |
| Statistics | `/api/statistics` | Grades, daily/weekly reading time, reading speed over time |
| Internal | `/internal/**` | Endpoints for the Python service (save attempts, stress-check progress, assignment data) |

## Project structure

```
src/main/java/com/example/readerai/
├── config/          # Security (JWT + internal API key), MinIO, RabbitMQ
├── controller/      # REST controllers
├── service/         # Business logic, RabbitMQ response handling
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities
├── dto/             # Request/response models (classroom, reading, statistics, rabbit)
├── converter/       # Entity ↔ DTO mapping
└── exception/       # Error handling
src/main/resources/
├── application.yml
├── catalog/         # Built-in book catalog (catalog.json + PDFs)
└── db/changelog/    # Liquibase changelogs
```
