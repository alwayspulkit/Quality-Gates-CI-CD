# CI/CD Quality Gates — Reference Implementation

A production-grade quality pipeline for a Spring Boot REST API. Every gate has a documented threshold and rationale — because a pipeline without decisions is just noise.

**Stack:** Java 17 · Spring Boot 3 · JUnit 5 · JaCoCo · SpotBugs · Testcontainers · k6 · GitHub Actions

---

## Pipeline Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Pull Request / Push                   │
└────────────────────────┬────────────────────────────────┘
                         │
            ┌────────────▼────────────┐
            │   Gate 1: Static        │  Checkstyle + SpotBugs
            │   Analysis              │  Fail fast, zero high bugs
            └────────────┬────────────┘
                         │
            ┌────────────▼────────────┐
            │   Gate 2: Unit Tests    │  JUnit 5 + JaCoCo
            │   + Coverage            │  80% line coverage enforced
            └────────────┬────────────┘
                         │
           ┌─────────────┴──────────────┐
           │                            │
┌──────────▼──────────┐   ┌────────────▼────────────┐
│  Gate 3: Integration │   │  Gate 4: Security        │
│  Tests               │   │  OWASP Dep Check         │
│  Full HTTP stack     │   │  No critical CVEs        │
└──────────┬──────────┘   └────────────┬────────────┘
           │                            │
           └─────────────┬──────────────┘
                         │
            ┌────────────▼────────────┐
            │   Gate 5: Performance   │  k6 load test
            │   p95 < 500ms           │  error rate < 1%
            │   error rate < 1%       │
            └────────────┬────────────┘
                         │
            ┌────────────▼────────────┐
            │   Summary + Merge       │
            └─────────────────────────┘
```

---

## Quality Gates At A Glance

| Gate | Tool | Threshold | Blocks |
|---|---|---|---|
| Static Analysis | Checkstyle + SpotBugs | Zero high-severity issues | Everything |
| Unit Coverage | JaCoCo | ≥ 80% line coverage | Integration + Security |
| Integration | Spring MockMvc | All tests pass | Performance |
| Security | OWASP Dep Check | No CVEs with CVSS ≥ 9 | Performance |
| Performance | k6 | p95 < 500ms, errors < 1% | Merge |

**[Full rationale for every threshold →](quality-gates.md)**

---

## Running Locally

**Prerequisites:** Java 17, Maven, Docker (for Testcontainers), k6

```bash
# Unit tests + coverage check
mvn test jacoco:check

# Integration tests (requires Docker for Testcontainers)
mvn test -Dtest="**/integration/**"

# Static analysis
mvn checkstyle:check spotbugs:check

# Performance test (requires running app)
java -jar target/*.jar &
k6 run tests/performance/load-test.js
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/books` | List all books |
| GET | `/api/books/{id}` | Get book by ID |
| POST | `/api/books` | Create a book |
| PUT | `/api/books/{id}` | Update a book |
| DELETE | `/api/books/{id}` | Delete a book |

---

## Setup

1. Add `NVD_API_KEY` to GitHub Secrets (free at [nvd.nist.gov](https://nvd.nist.gov/developers/request-an-api-key))
2. Push to a branch — the pipeline triggers automatically on PR

---

## Related Writing

- [Stop Chasing Individual Failures](https://pulkitchaturvedi.substack.com/p/stop-chasing-individual-failures) — the thinking behind systematic quality analysis
- [The Career Shortcut Nobody Talks About](https://pulkitchaturvedi.substack.com/p/the-career-shortcut-nobody-talks) — on identifying and owning org-level problems
