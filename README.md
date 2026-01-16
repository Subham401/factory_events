# Factory Events Backend System

This backend system ingests machine-generated events from a factory, applies strict validation and deduplication rules, and provides analytical statistics over user-defined time windows.
It is designed to be **thread-safe**, **scalable**, and **production-realistic**, using **PostgreSQL** as the single source of truth.

---

# 1. Architecture

## High-Level Layered Architecture

```
Controller Layer  →  Service Layer  →  Repository Layer  →  PostgreSQL
(HTTP endpoints)      (business rules)     (data access)      (source of truth)
```

### Controller Layer

* Exposes REST endpoints.
* Performs **no business logic**.
* Delegates computation to services.

### Service Layer

* Implements **validation**, **deduplication**, **update rules**, **stats aggregation**, and **time-window logic**.
* Uses `@Transactional` to enforce consistency.
* All business rules live here.

### Repository Layer

* Implements persistence using Spring Data JPA.
* PostgreSQL handles indexing, locking, constraints.

### Database

* Stores all events.
* Enforces primary key on `event_id` ensuring deduplication.
* Provides row-level locks + MVCC for concurrency correctness.

---

# 2. Dedupe / Update Logic

Events are uniquely identified by `eventId`.

When an event arrives:

### Step 1 — Validation

Reject if:

* `durationMs < 0`
* `durationMs > 6 hours`
* `eventTime > now + 15 minutes`

### Step 2 — Compute Payload Hash

A reproducible string-based hash is generated from:
`eventId | machineId | eventTime | durationMs | defectCount | factoryId | lineId`

### Step 3 — Find existing event (if any)

Use PK lookup: `findById(eventId)`.

### Step 4 — Decide outcome

| Condition                                | Outcome      |
| ---------------------------------------- | ------------ |
| Identical payload hash                   | **DEDUPED**  |
| Different payload + newer `receivedTime` | **UPDATED**  |
| Different payload + older `receivedTime` | **IGNORED**  |
| No existing event                        | **ACCEPTED** |

### Why `receivedTime`?

* Represents ingestion ordering.
* Prevents out-of-order sensor messages from overwriting newer data.

---

# 3. Thread-Safety

Thread safety is guaranteed with **zero Java locks**.

### Mechanisms Used

#### ✅ 1. PostgreSQL Primary Key Constraint

* Prevents duplicate `eventId` inserts during concurrent ingestion.

#### ✅ 2. Transactional Read–Modify–Write

`@Transactional` ensures:

* Consistent snapshot
* Atomic update
* No partial modifications

#### ✅ 3. PostgreSQL MVCC (Multi-Version Concurrency Control)

* Readers never block writers.
* Writers lock individual rows only.
* Stats queries remain consistent under concurrent ingestion.

### Why no Java synchronization?

Database-level locking is:

* safer
* scalable
* deadlock-free when used properly

This is real backend engineering practice.

---

# 4. Data Model

## SQL Schema

```sql
CREATE TABLE events (
    event_id      VARCHAR(64) PRIMARY KEY,
    machine_id    VARCHAR(64) NOT NULL,
    event_time    TIMESTAMPTZ NOT NULL,
    received_time TIMESTAMPTZ NOT NULL,
    duration_ms   BIGINT NOT NULL,
    defect_count  INT NOT NULL,
    payload_hash  VARCHAR(64) NOT NULL,
    factory_id    VARCHAR(32),
    line_id       VARCHAR(32)
);

CREATE INDEX idx_events_machine_time
ON events(machine_id, event_time);
```

### Notes

* `event_id` as **PK** enables fast dedupe.
* `TIMESTAMPTZ` avoids timezone bugs.
* `(machineId, eventTime)` helps stats queries.

---

# 5. Performance Strategy

Goal: **Process 1000 events under 1 second**.

### What was done:

#### ✔ 1. Validation before DB access

Reduces DB load.

#### ✔ 2. Payload hashing instead of deep comparison

Makes dedupe checks O(1).

#### ✔ 3. JPA PK lookup is indexed & fast

Single-row operations.

#### ✔ 4. Small transactions

Each event atomic; batch not rolled back entirely.

#### ✔ 5. HikariCP tuned

* Max pool size = 10
* Minimal idle connections = 2

### Benchmark Summary

* 1000 events processed in **350–600ms** consistently.

(Details in `BENCHMARK.md`)

---

# 6. Edge Cases & Assumptions

### Edge Case A — `defectCount = -1`

* Stored normally.
* **Ignored** for defect-related metrics.

Reason: requirement says `-1` means unknown.

---

### Edge Case B — Time Window Boundaries

* `start` inclusive
* `end` exclusive

Reason: avoids double counting across adjacent windows.

---

### Edge Case C — Future Event

* Events beyond `now + 15 min` are rejected.

Reason: prevents bad device clocks corrupting data.

---

### Edge Case D — Concurrent Ingestion

* Multiple threads inserting same event results in **1 row**.
* Deterministic winner based on newest `receivedTime`.

---

### Assumptions

* `eventId` is globally unique for the system.
* Events are assumed small JSON payloads.
* Stats queries operate on manageable ranges (few hours to few days).

---

# 7. Setup & Run Instructions

## Prerequisites

* Java 17
* PostgreSQL 14+
* Maven

## Step 1 — Create Database

```sql
CREATE DATABASE factory_events;
```

## Step 2 — Configure `application.properties`

```
spring.datasource.url=jdbc:postgresql://localhost:5432/factory_events
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
```

## Step 3 — Run Application

```bash
mvn spring-boot:run
```

---

# 8. What I Would Improve With More Time

These improvements focus on scalability and production-readiness:

### 1. Kafka-based ingestion

Batching and queueing for large event volumes.

### 2. Pre-aggregated statistics

Materialized views for faster analytics.

### 3. SHA-256 payload hashing

Stronger collision resistance.

### 4. Stored procedures for bulk ingestion

Reduce ORM overhead for 10k+ event batches.

### 5. Caching stats results

Avoid re-computation on repeated queries.

### 6. Authentication & rate limiting

Protect ingestion endpoint in production.