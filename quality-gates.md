# Quality Gates — Decisions & Rationale

This document explains every gate in the pipeline: what it checks, why the threshold is set where it is, and what happens when it fails. A pipeline without documented decisions is just automation. This is the thinking.

---

## Gate 1 · Static Analysis

### Checkstyle
**What:** Enforces naming conventions, bans unused imports, limits method length (50 lines) and parameter count (7).

**Why these thresholds:**
- 50-line methods are a forcing function for single responsibility. If a method exceeds this, it's almost always doing two things.
- 7 parameters signals a missing abstraction — that's a config object or builder waiting to happen.
- Unused imports are noise that makes diffs harder to review and signals copy-paste development.

**On failure:** The build fails at `validate` phase, before any tests run. Fast feedback is cheaper than slow feedback.

---

### SpotBugs
**What:** Static bytecode analysis for common bug patterns. Configured at `High` threshold — only high-severity issues fail the build.

**Why High and not Medium:**
Medium-severity SpotBugs findings have a high false-positive rate in Spring applications due to proxy-generated classes. Failing on Medium wastes developer time chasing ghosts. High-severity findings (null dereferences, SQL injection patterns, resource leaks) are almost always real.

**On failure:** A SpotBugs XML report is uploaded as an artifact for investigation. Fix the root cause, not the suppression.

---

## Gate 2 · Unit Tests + Coverage

### JUnit 5
**What:** Unit tests covering service layer logic with mocked dependencies.

**Why unit tests before integration:**
Unit tests are 10-100x faster than integration tests. Running them first gives sub-60-second feedback on logic errors. Integration tests are not a substitute — they catch different failure modes.

### JaCoCo — 80% Line Coverage
**What:** Fails the build if less than 80% of lines are covered by unit tests.

**Why 80% and not 100%:**
100% is a vanity metric that incentivises testing getters and setters. 80% forces coverage of all meaningful branches (happy path, error path, edge cases) while leaving room for boilerplate that doesn't need testing.

**Why not lower:**
Below 75%, the coverage number stops being predictive. Teams that drop below this threshold consistently ship bugs caught by later stages — which is expensive.

**What 80% doesn't catch:** Integration-level failures, race conditions, infrastructure-dependent behaviour. That's what the next gates are for.

---

## Gate 3 · Integration Tests

**What:** Full Spring Boot context with an H2 in-memory database. Tests the HTTP layer — request validation, response codes, error handling — end to end.

**Why after unit tests:**
Integration tests take 3-10x longer. They run only when unit tests pass, so the slow gate doesn't run unnecessarily.

**Why H2 and not Testcontainers here:**
For controller-level integration tests, H2 gives sufficient confidence at much lower CI cost. Testcontainers (real PostgreSQL) is reserved for tests that touch database-specific behaviour (query plans, constraints, migrations).

**What this gate catches that unit tests miss:**
- Spring Security misconfigurations
- JSON serialization/deserialization edge cases
- Request validation failures
- HTTP status code correctness

---

## Gate 4 · Security — OWASP Dependency Check

**What:** Scans all dependencies against the NVD (National Vulnerability Database). Fails on CVSS score ≥ 9 (Critical).

**Why CVSS ≥ 9 and not lower:**
CVSS 7+ (High) generates too much noise in active projects — many are theoretical vulnerabilities with no applicable attack vector. Critical (9+) vulnerabilities are exploitable in realistic conditions and must be addressed immediately.

**Suppression policy:**
Any suppression in `config/owasp-suppressions.xml` must include:
1. The CVE identifier
2. Why it doesn't apply to this codebase
3. The review date

Suppressions without documentation are technical debt that becomes a security incident.

**NVD API Key:**
Store as `NVD_API_KEY` in GitHub Secrets. Unauthenticated NVD calls are rate-limited and will cause flaky builds.

---

## Gate 5 · Performance — k6

**What:** Simulates 10 concurrent users for 2 minutes against a running instance. Measures:
- p95 response time (95th percentile)
- Error rate

### Thresholds

| Metric | Gate | Rationale |
|---|---|---|
| `p95 latency` | < 500ms | Industry baseline for REST APIs under moderate load. Exceeding this under 10 concurrent users signals a problem that will be catastrophic at real scale. |
| `error rate` | < 1% | At 10 concurrent users, even a 1% error rate is unacceptable. This threshold has zero tolerance for infrastructure or logic errors under load. |

**Why p95 and not p99:**
p99 in a 2-minute test window is dominated by outliers (GC pauses, cold starts). p95 is stable enough to be meaningful in short test windows and representative of the majority user experience.

**Why this runs last:**
Performance tests require a running application and real infrastructure. They're the most expensive gate to run — 2+ minutes — and only make sense if all prior gates have passed.

---

## Gate Sequencing Logic

```
static-analysis
      │
      ▼
unit-tests ──────────────────────────┐
      │                              │
      ▼                              ▼
integration-tests              security-scan
      │                              │
      └──────────────┬───────────────┘
                     ▼
             performance-tests
                     │
                     ▼
             pipeline-summary
```

**Static analysis and unit tests block everything** — no point running expensive gates if basic quality isn't met.

**Integration and security run in parallel** — they're independent and both feed into performance.

**Performance runs last** — it's the most expensive and requires the app to be running.

---

## What's Not in This Pipeline (and why)

| Missing | Why it's a deliberate omission |
|---|---|
| Mutation testing (PIT) | High value, high cost. Add when the team is consistently hitting 80% coverage and wants to improve test quality, not just quantity. |
| Contract tests (Pact) | Requires a broker infrastructure. Worth adding when there are multiple consuming services. |
| E2E browser tests | Playwright setup is included but not wired into CI here — too flaky at the PR level. Better on a nightly schedule against a staging environment. |
| Load spike/soak tests | The k6 script tests sustained load, not spikes or extended soak. These belong in a separate nightly workflow. |
