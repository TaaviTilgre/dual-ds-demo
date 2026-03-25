# Dual Datasource Demo — Kotlin Spring Boot

A minimal Spring Boot (Kotlin) demo showing how to configure **read/write datasource routing**, **ShedLock**, and **Resilience4j circuit breakers** using standard Spring mechanisms.

## What This Demonstrates

### 1. Read/Write Datasource Routing

Instead of Spring Boot's default single-datasource auto-configuration, this project manually wires two HikariCP connection pools behind a `RoutingDataSource` (extends Spring's `AbstractRoutingDataSource`).

Routing is automatic based on `@Transactional`:
- `@Transactional(readOnly = true)` → **read** datasource (replica)
- `@Transactional` → **write** datasource (primary)

```
@Transactional(readOnly = true)       @Transactional
        │                                    │
        ▼                                    ▼
   RoutingDataSource.determineCurrentLookupKey()
        │                                    │
   READ detected                       WRITE (default)
        │                                    │
        ▼                                    ▼
  ReadHikariPool                     WriteHikariPool
   (read replica)                    (primary DB)
```

**Key files:**
- [`RoutingDataSource.kt`](src/main/kotlin/com/example/demo/config/RoutingDataSource.kt) — routing logic
- [`DataSourceConfig.kt`](src/main/kotlin/com/example/demo/config/DataSourceConfig.kt) — datasource, EntityManagerFactory, and TransactionManager wiring
- [`DataSourceType.kt`](src/main/kotlin/com/example/demo/config/DataSourceType.kt) — enum for lookup keys

### 2. ShedLock (Distributed Scheduled Task Locking)

[ShedLock](https://github.com/lukas-krecan/ShedLock) ensures scheduled tasks run only once across multiple application instances. It uses a database table on the **write** datasource to coordinate locks.

**Key files:**
- [`ShedLockConfig.kt`](src/main/kotlin/com/example/demo/config/ShedLockConfig.kt) — `JdbcTemplateLockProvider` wired to the write datasource
- [`ShedLockTableInitializer.kt`](src/main/kotlin/com/example/demo/config/ShedLockTableInitializer.kt) — creates the `shedlock` table on startup
- [`ScheduledTasks.kt`](src/main/kotlin/com/example/demo/service/ScheduledTasks.kt) — example scheduled task with `@SchedulerLock`

### 3. Resilience4j Circuit Breakers

Separate circuit breaker instances for read and write operations, configured via `application.yml` using standard Spring Boot auto-configuration.

- **`writeService`** — protects write operations (create, delete)
- **`readService`** — protects read operations (list, get by id)

Each has fallback methods that return safe defaults when the circuit is open.

**Key file:**
- [`OrderService.kt`](src/main/kotlin/com/example/demo/service/OrderService.kt) — `@CircuitBreaker` annotations with fallbacks

## API Endpoints

| Method   | URL                | Routed To       | Description        |
|----------|--------------------|-----------------|--------------------|
| `GET`    | `/api/orders`      | Read datasource | List all orders    |
| `GET`    | `/api/orders/{id}` | Read datasource | Get order by ID    |
| `POST`   | `/api/orders`      | Write datasource| Create an order    |
| `DELETE` | `/api/orders/{id}` | Write datasource| Delete an order    |
| `GET`    | `/actuator/health` | —               | Health + CB status |

**Example request:**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"product": "Widget", "quantity": 5}'
```

## Running

```bash
./gradlew bootRun
```

Both datasources use in-memory H2, so no external database setup is needed.

## Tech Stack

- Kotlin 1.9 / Java 21
- Spring Boot 3.4
- Spring Data JPA + Hibernate
- HikariCP (connection pooling)
- H2 (in-memory, for demo)
- Resilience4j 2.2 (circuit breakers)
- ShedLock 6.2 (distributed lock)

## Note

Since both H2 databases are separate in-memory instances, data written to the write DB won't appear when reading from the read DB. In a real setup the read datasource would point to a replica that replicates from the primary. To test locally, you can point both datasource URLs at the same H2 instance.
