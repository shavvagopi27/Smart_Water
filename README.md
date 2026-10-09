# 💧 Smart Water Usage Monitoring & Automated Billing Platform

A full-stack apartment-community platform that monitors household water consumption, manages meter readings and
bulk water purchases, bills households with **configurable tiered tariffs**, shares apartment water costs fairly,
detects abnormal usage, generates **PDF invoices**, sends **e-mail notifications** and provides separate
**resident** and **admin** dashboards.

> **Build status – please read:** see [Verification status](#verification-status) for exactly what was and was not
> executed when this project was generated.

## 1. Features
- JWT authentication, BCrypt password hashing, role-based access (**ADMIN**, **RESIDENT**); residents can never read another household's data.
- Apartments, households, residents, water meters (one per household).
- Manual daily readings + **bulk CSV upload** with validation, duplicate detection and a per-row error report.
- Tiered tariffs stored in the database (nothing hard-coded) – admin-configurable.
- Bulk water purchases (tanker / municipal) with **proportional cost distribution** and **flat-area fallback**.
- Billing cycles `OPEN → FINALIZED → ARCHIVED`; finalized cycles lock readings, purchases and invoices.
- Itemised invoices + professional **PDF** (Apache PDFBox).
- Alert engine (`@Scheduled`): usage threshold, high consumption, statistical anomaly (mean + 2σ), potential leakage pattern.
- E-mail notifications (bill generated / invoice available / alerts / weekly tips) – safe by default (log-only until you enable SMTP).
- React dashboards with Recharts for residents and admins; Swagger/OpenAPI docs.

## 2. Architecture
```
React (Vite)  ──HTTP/JSON──▶  Spring Boot REST API  ──JPA/Flyway──▶  PostgreSQL
                               ├─ controller   (HTTP only, no business logic)
                               ├─ service      (BillingService, AlertService, WaterUsageService, …)
                               │   └─ TariffCalculator · CostDistributor · AnomalyDetector (pure, unit-tested engines)
                               ├─ repository   (Spring Data JPA)
                               ├─ security     (JWT filter, role checks, ownership checks)
                               └─ PDF / E-mail services, @Scheduled jobs
```
Entities are never returned from the API – every endpoint uses request/response DTOs (Java records).

## 3. Technology stack
Java 21 · Spring Boot 3.3 · Spring Web · Spring Data JPA/Hibernate · Spring Security 6 + JJWT · Jakarta Validation · PostgreSQL 16 ·
Flyway · Apache PDFBox 3 · Spring Mail · springdoc OpenAPI · JUnit 5 / Mockito / MockMvc (H2 for tests only) · Lombok (boilerplate only) ·
React 18 · Vite 5 · React Router 6 · Axios · Recharts · plain CSS.

## 4. Database design
13 tables created by Flyway migrations `V1…V11` (`backend/src/main/resources/db/migration`):
`users, apartments, households, water_meters, water_usage_logs, tariff_plans, tariff_tiers, billing_cycles,
water_purchases, invoices, invoice_items, alerts, water_saving_tips`.

```
apartments 1─* households 1─1 water_meters
apartments 1─* tariff_plans 1─* tariff_tiers
apartments 1─* billing_cycles 1─* invoices 1─* invoice_items      households 1─* invoices
apartments 1─* water_purchases (optional link to billing_cycles)
households 1─* water_usage_logs (also *─1 water_meters)           households 1─* alerts
users *─1 households / apartments
```
Every table has `created_at`/`updated_at`, primary/foreign keys, CHECK constraints (e.g. readings ≥ 0, tier ranges, status values)
and indexes on foreign keys / lookup columns. Notable uniqueness rules: one reading per meter per day, one invoice per household per
cycle, one alert per household/type/day, one meter per household.

## 5. Setup & running

### Option A – Docker (recommended)
```bash
docker compose up --build
```
| Service | URL |
|---|---|
| Web app | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

Stop with `Ctrl+C`; remove data with `docker compose down -v`.

### Option B – run the pieces locally
**PostgreSQL** (Docker one-liner):
```bash
docker run -d --name smartwater-db -e POSTGRES_DB=smartwater -e POSTGRES_USER=smartwater \
  -e POSTGRES_PASSWORD=local_demo_db_password -p 5432:5432 postgres:16-alpine
```
**Backend** (needs JDK 21 + Maven):
```bash
./scripts/run-backend-local.sh        # sets the local demo env vars and runs mvn spring-boot:run
# or manually:
cd backend
export DB_PASSWORD=local_demo_db_password JWT_SECRET=local-demo-only-jwt-secret-please-change-0123456789
mvn spring-boot:run
```
**Frontend** (Node 20+):
```bash
cd frontend && npm install && npm run dev      # http://localhost:5173  (Vite proxies /api → localhost:8080)
```

### Environment variables
| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | password required | `jdbc:postgresql://localhost:5432/smartwater` / `smartwater` / – | Database |
| `JWT_SECRET` | **yes** (≥ 32 chars) | – | HMAC key for tokens |
| `JWT_EXPIRATION_MINUTES` | no | 120 | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000` | Allowed browser origins |
| `SEED_DEMO_DATA` | no | `true` | Seed demo data on an **empty** DB. Use `false` outside local demos |
| `MAIL_ENABLED` | no | `false` | `false` = e-mails only logged |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS`, `MAIL_FROM` | when mail enabled | – | SMTP settings |
| `ALERT_HIGH_USAGE_KL` | no | `1.0` | Daily kL per household that triggers a threshold alert |
| `ALERT_HIGH_USAGE_FACTOR`, `ALERT_STDDEV_MULTIPLIER`, `ALERT_CRON` | no | `2.0`, `2.0`, `0 0 6 * * *` | Alert tuning |
| `BILLING_AUTO_CYCLE_ENABLED` / `BILLING_AUTO_CYCLE_CRON` | no | `false` / 1st of month 02:00 | Auto-create last month's cycle + draft invoices |
| `TIPS_EMAIL_ENABLED` / `TIPS_EMAIL_CRON` | no | `false` / Mondays 09:00 | Weekly tips e-mail |
| `PAYMENT_INSTRUCTIONS` | no | generic text | Printed on invoices |

Root `.env` (used by Docker Compose) contains **local demo placeholders only** – replace them for anything real.

## 6. Sample (demo) credentials – LOCAL USE ONLY
Created automatically on an empty database when `SEED_DEMO_DATA=true`:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@smartwater.local` | `Admin@12345` |
| Resident (Flat A-101) | `resident1@smartwater.local` | `Resident@12345` |
| Residents A-102…A-105 | `resident2…5@smartwater.local` | `Resident@12345` |

Demo data: 1 apartment (Green Valley Residency), 5 households, 5 residents, 1 admin, 5 meters (WM-1001…1005), ~2 months of daily
readings, a 3-tier tariff (0–10 kL ₹20, 10–20 kL ₹30, 20+ kL ₹40), 3 water purchases, a finalized last-month cycle with invoices,
an open current-month cycle with draft invoices, and alerts (a spike in A-104, a 3-day elevated pattern in A-105).

## 7. API documentation
Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`. Click **Authorize** and paste the JWT from `POST /api/auth/login`.
Every operation documents 400/401/403/404/409 error responses. All errors share one shape:
```json
{ "success": false, "message": "Household not found", "timestamp": "2026-10-09T10:15:30Z", "status": 404 }
```
(validation errors add an `errors` object keyed by field).

| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/register` · `POST /api/auth/login` · `GET /api/auth/me` |
| Apartments (admin) | `POST/GET /api/apartments` · `GET/PUT/DELETE /api/apartments/{id}` |
| Households | `POST/GET /api/households` · `GET/PUT/DELETE /api/households/{id}` |
| Residents | `POST/GET /api/households/{id}/residents` · `DELETE /api/households/{hid}/residents/{uid}` · `GET /api/users` · `GET/PUT /api/users/{id}` |
| Meters | `POST/GET /api/meters` · `GET/PUT /api/meters/{id}` |
| Usage | `POST /api/water-usage` · `POST /api/water-usage/upload` · `GET /api/water-usage/{householdId}` · `/daily` · `/monthly` |
| Tariffs (admin) | `POST/GET /api/tariffs` · `GET/PUT/DELETE /api/tariffs/{id}` |
| Purchases (admin) | `POST/GET /api/water-purchases` · `GET/PUT/DELETE /api/water-purchases/{id}` |
| Billing cycles (admin) | `POST/GET /api/billing-cycles` · `GET /{id}` · `POST /{id}/generate` · `PUT /{id}/finalize` · `PUT /{id}/archive` |
| Invoices | `GET /api/invoices` · `GET /api/invoices/{id}` · `GET /api/invoices/{id}/pdf` · `PUT /{id}/paid` · `POST /{id}/email` |
| Alerts | `GET /api/alerts` · `PUT /api/alerts/{id}/read` · `POST /api/alerts/run-detection` (admin) |
| Tips / Dashboards | `GET/POST/PUT/DELETE /api/tips` · `GET /api/dashboard/resident` · `GET /api/dashboard/admin` |

### Quick Postman / curl walkthrough
```bash
# 1. login
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@smartwater.local","password":"Admin@12345"}'
# → {"token":"eyJ...","user":{"id":1,"name":"Apartment Admin","email":"admin@smartwater.local","role":"ADMIN",...}}

# 2. record a reading (use the token)
curl -s -X POST localhost:8080/api/water-usage -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"householdId":1,"meterId":1,"readingDate":"2026-10-08","meterReading":1215.000}'
# → 201 {"id":..,"readingDate":"2026-10-08","meterReading":1215.000,"consumption":15.000,...}

# 3. CSV upload (columns: meterId,householdId,readingDate,meterReading)
curl -s -X POST localhost:8080/api/water-usage/upload -H "Authorization: Bearer $TOKEN" -F file=@readings.csv
# → {"totalRecords":6,"successfulRecords":2,"failedRecords":3,"duplicateRecords":1,"errors":[{"row":4,"message":"…"}]}

# 4. generate invoices for a cycle, then finalize
curl -s -X POST localhost:8080/api/billing-cycles/1/generate -H "Authorization: Bearer $TOKEN"
curl -s -X PUT  localhost:8080/api/billing-cycles/1/finalize  -H "Authorization: Bearer $TOKEN"
```

## 8. Billing calculation
For each active household in a billing cycle (`BillingService`):
1. **Consumption** = sum of daily consumption in the cycle (`reading − previous reading`, first reading vs the meter's initial reading).
   Example: previous 1200 kL, current 1215 kL → **15 kL**.
2. **Tiered tariff** (from the database; tier covers `(from, to]`, last tier open-ended):
   15 kL → 10 × ₹20 + 5 × ₹30 = **₹350**.
3. **Shared purchase cost** = all purchases in the cycle (linked to it, or unlinked and dated inside it), split by `CostDistributor`:
   * Metered households share the pool **in proportion to measured consumption** (A 100, B 200, C 300, D 400 kL of 1000 kL and ₹20,000 → ₹2,000 / ₹4,000 / ₹6,000 / ₹8,000).
   * **Fallback (no meter or no readings in the cycle):** the household is charged by **flat area** – its slice is `totalCost × area / total area of all households`;
     the remaining cost is shared among metered households by consumption. If nobody has readings, everything is split by area.
   * Amounts are rounded to 2 decimals; the rounding difference is added to the largest share so shares always sum exactly to the total.
4. **Adjustments** (optional credit/debit with a reason, passed when generating).
5. **Final amount** = water charge + shared cost + adjustment → e.g. 350 + 120 + 0 = **₹470**. Stored as an itemised invoice.

Generating is repeatable while the cycle is `OPEN` (drafts are replaced). `finalize` locks the cycle (409 on further changes, readings and purchases dated inside it are rejected) and e-mails the bills.

## 9. Anomaly detection
Runs daily (`@Scheduled`, `ALERT_CRON`) and on demand (`POST /api/alerts/run-detection`). For each household's latest daily reading:
* **Threshold**: usage > `ALERT_HIGH_USAGE_KL`.
* **High consumption**: usage > 2 × the apartment's average household daily usage.
* **Statistical anomaly**: usage > `mean + 2 × σ` of the household's previous 30 days (population standard deviation, ≥ 7 days of history required).
  Message: *"Potential abnormal usage detected."*
* **Potential leakage pattern**: three consecutive days each > 1.5 × the household's normal level.

These are indicators only – the system never claims a leak is certain. Each (household, type, day) is alerted once.

## 10. Tests
```bash
cd backend && mvn test
```
* Unit: tariff calculation, cost distribution (incl. fallback and rounding), anomaly detection, `WaterUsageService` (Mockito).
* Integration (Spring Boot + MockMvc + in-memory H2): registration, login, JWT/role authorization, household CRUD, meter readings (valid / duplicate / lower / negative / future),
  CSV upload, tariff validation, full billing flow (₹470 example), PDF generation, cycle locking, area fallback, anomaly alert, resident data isolation.

## 11. Screenshots
_Add your screenshots here_ — `docs/screenshots/login.png`, `resident-dashboard.png`, `admin-dashboard.png`, `invoice-pdf.png`.

## 12. Verification status
This project was generated in a sandbox **without access to Maven Central**, so:

| Part | Status |
|---|---|
| Flyway SQL migrations V1–V11 | ✅ executed on real PostgreSQL – all 13 tables created, tips seeded |
| Tariff / cost-distribution / anomaly engines | ✅ compiled and run against the spec examples (₹350, ₹2000/4000/6000/8000, sums exact) |
| Frontend | ✅ `npm install` + `npm run build` succeeded (production bundle) |
| Java syntax of all backend + test sources | ✅ parsed with `javac` (no syntax errors) |
| Spring Boot backend full compile, `mvn test`, Docker build, end-to-end run | ⚠️ **not executed in the sandbox** (dependencies could not be downloaded) |

Please run `cd backend && mvn test` and `docker compose up --build` on your machine first. If anything fails, send me the error output
and it can be fixed quickly – the most likely sources are small compile/mapping details that only the real toolchain reveals.

## 13. Known limitations
- Single-currency (₹) and metric (kL) only; PDF prints "Rs." because standard PDF fonts lack the rupee glyph.
- Backdated readings are rejected (readings must be entered in date order per meter).
- No online payment gateway: invoices have a manual `PAID` status.
- Admins see all apartments (no per-apartment admin scoping). Public registration creates residents without a household until an admin links them.
- JWT is stored in `localStorage` (standard for SPAs but XSS-sensitive); no refresh tokens or password-reset flow.
- Alert/E-mail jobs are in-process schedulers (run once per instance; use ShedLock or similar if you scale out).

## 14. Future enhancements
Smart-meter / IoT ingestion API, per-apartment admin scoping, payment gateway + receipts, password reset & e-mail verification,
SMS/WhatsApp alerts, multi-currency/locale, code-splitting and i18n in the frontend, ShedLock for clustered schedulers, Testcontainers-based tests.
