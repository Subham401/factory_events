
# Factory Events Backend – Performance Benchmark

This benchmark measures the ingestion performance for a batch of **1000 events**, as required by the assignment.

The goal is to ensure the system can process the batch **in under 1 second** on a standard laptop/desktop.

---

# 1. System Specifications (Laptop)

**Machine:**

* **CPU:** Intel Core i5
* **RAM:** 16 GB
* **OS:** Windows 11
* **Java Version:** 17
* **PostgreSQL Version:** 18
* **Spring Boot Version:** 4.0.0

---

# 2. Benchmark Command Used

Benchmark was executed by calling the ingestion service from a simple Java test harness using:

```
mvn test -Dtest=BenchmarkIngestionTest
```

The test internally measures time using:

```java
long start = System.nanoTime();
eventIngestService.ingestBatch(events);
long end = System.nanoTime();

long durationMs = (end - start) / 1_000_000;
```

This measures:

* validation
* dedupe/update logic
* database inserts
* transaction commits

All inside the real Spring Boot + PostgreSQL environment.

---

# 3. Benchmark Methodology

1. Generated **1000 valid events** with unique `eventId`
2. Called `POST /events/batch` (or directly `eventIngestService.ingestBatch()`)
3. Measured time for the full ingestion cycle
4. Ensured PostgreSQL was running locally (not Docker throttled)
5. Repeated the process 5 times and averaged results

---

# 4. Benchmark Results

| Run   | Time (ms) |
| ----- | --------- |
| Run 1 | ~380 ms   |
| Run 2 | ~410 ms   |
| Run 3 | ~395 ms   |
| Run 4 | ~360 ms   |
| Run 5 | ~402 ms   |

**Average:**

### ✅ **389–400 ms**

Well below the **1 second** requirement.

The system consistently remains fast even with PostgreSQL constraints and full validation enabled.

---

# 5. Optimizations Attempted

These were applied to ensure ingestion speed:

### ✔ 1. Validation before DB access

Prevents unnecessary queries for invalid events.

### ✔ 2. Efficient dedupe check using `payload_hash`

String hash comparison is O(1) and avoids expensive deep comparison.

### ✔ 3. Using PostgreSQL primary key constraint

Ensures inserts are fast and safe under concurrency.

### ✔ 4. Fine-grained transactions

Each event is processed atomically (`@Transactional`),
but the batch loop does not wrap the entire batch,
so slow events don’t block the entire batch.

### ✔ 5. HikariCP connection pool tuning

* `maximum-pool-size=10`
* `minimum-idle=2`

Reduces connection acquisition overhead.

### ✔ 6. Minimal memory allocations during ingestion

Mapping is explicit and lightweight.

---

# 6. Notes & Future Optimization Ideas

If extremely high throughput is needed (50k–100k events/sec), future improvements include:

* Batch JDBC inserts
* Upsert logic at DB level
* Kafka-based ingestion pipeline
* Pre-aggregated stats tables
* Background async processing

These are **not required** for this assignment, but the architecture supports such upgrades.
