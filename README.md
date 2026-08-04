# TradeFlow — Trade Reconciliation Dashboard

> A full-stack trade-reconciliation platform built during Deutsche Bank's TDI
> 2026 Graduate Technical Training Programme. TradeFlow manages trades,
> publishes lifecycle events through Kafka, records audit activity, and exposes
> operational metrics through Prometheus and Grafana.

## Quick start

### Prerequisites

- Docker Desktop running, with Docker Compose v2 available
- Git
- A GitHub personal access token with `read:packages` if the TradeFlow GHCR
  images are private

From the repository root:

```bash
cp .env.example .env

# Required only when GHCR asks for authentication.
echo "<github-token>" | docker login ghcr.io -u <github-username> --password-stdin

docker compose pull
docker compose up -d
docker compose ps
```

Wait until the backend and frontend show `healthy`. A cold start can take a
minute while PostgreSQL, Kafka, Liquibase, and the backend initialise.

Useful diagnostics:

```bash
docker compose logs -f backend
docker compose logs -f kafka
docker compose down                 # stop containers; keep database volumes
docker compose down -v              # stop containers and remove local data
```

`docker compose down -v` removes the local PostgreSQL and Grafana volumes, so
use it only when resetting the demo environment is intended.

## Open the platform

| URL | Purpose | Credentials |
| --- | --- | --- |
| <http://localhost:5173> | React operations dashboard | The demo UI uses the trader account automatically. |
| <http://localhost:8080/swagger-ui.html> | Swagger / OpenAPI API explorer | `trader` / `trader-pw` for write endpoints. |
| <http://localhost:8080/actuator/health> | Backend health check | None. |
| <http://localhost:9000> | Kafdrop — inspect Kafka topics and consumer groups | None. |
| <http://localhost:9090> | Prometheus | None. |
| <http://localhost:3000> | Grafana dashboards | `admin` / `admin` by default; configurable in `.env`. |

The API uses HTTP Basic authentication. The built-in demo accounts are:

| User | Password | Roles |
| --- | --- | --- |
| `viewer` | `viewer-pw` | Read-only API access |
| `trader` | `trader-pw` | Read and write API access |
| `admin` | `admin-pw` | Read, write, and protected actuator access |

These are development/demo credentials only; they are not suitable for a
production deployment.

## Deploy a verified image

CI publishes backend and frontend images to GitHub Container Registry (GHCR)
after a successful push to `develop` or `main`. The normal demo-laptop flow
is exactly the quick start above: log in if necessary, pull, then start the
Compose stack.

To pin a known-good build, replace both `:latest` tags in `.env` with the
same full commit SHA reported by the successful GitHub Actions run, then pull
and start again:

```bash
docker compose pull
docker compose up -d
```

Pinning both images prevents a later CI build from changing the application
halfway through a demo.

## Architecture

See [the architecture document](./docs/architecture.md) for the runtime
component diagram and the GitHub Actions → GHCR → demo-laptop delivery flow.

At runtime, the React UI calls the Spring Boot API. The API persists trades in
PostgreSQL and publishes trade events to Kafka. Independent consumer groups
handle logging, reconciliation triggering, and audit persistence. Prometheus
scrapes backend metrics and Grafana visualises the provisioned dashboards.

## API contract

All API endpoints are rooted at `/api/v1`. The Swagger UI is the interactive
source of truth, including request schemas and response examples.

| Method | Path | Required role | Description |
| --- | --- | --- | --- |
| `GET` | `/trades` | `VIEWER` | List trades; optional `status`, `from`, and `to` filters. |
| `POST` | `/trades` | `TRADER` | Create a trade and publish a `CREATED` event. |
| `PUT` | `/trades/{id}/status` | `TRADER` | Update a trade's status. |
| `DELETE` | `/trades/{id}` | `TRADER` | Soft-delete a trade by setting its status to `CANCELLED`. |
| `POST` | `/recon/run` | `TRADER` | Trigger the available reconciliation summary run. |
| `GET` | `/recon/results` | `VIEWER` | List recon breaks; supports `status`, `counterpartyId`, and pagination. |
| `PUT` | `/recon/{id}/resolve` | `TRADER` | Mark a recon break as resolved. |

The public operational endpoints are `/actuator/health`, `/actuator/info`,
`/actuator/prometheus`, and the Swagger/OpenAPI paths. Other actuator
endpoints require the `ADMIN` role.

## Repository layout

| Path | Contents |
| --- | --- |
| [`backend/`](./backend) | Spring Boot API, Liquibase migrations, Kafka producers/consumers, and tests. |
| [`frontend/`](./frontend) | React/Vite dashboard served by nginx in the Docker image. |
| [`monitoring/`](./monitoring) | Prometheus configuration and provisioned Grafana dashboards. |
| [`docs/`](./docs) | Architecture and AI-review documentation. |
| [`docker-compose.yml`](./docker-compose.yml) | The eight-service local/demo stack. |
| [`student-guides/`](./student-guides) | Day-by-day ticket requirements and reference hints. |

## Local development and verification

For backend development, Java 17 and a Kafka broker on port 9092 are required
for the full test suite. Start the Compose Kafka dependency first. The Maven
wrapper is invoked through `sh` because its executable file mode is not
tracked in this repository:

```bash
docker compose up -d zookeeper kafka
cd backend
sh ./mvnw clean verify
```

For frontend development, Node.js 20+ is required:

```bash
cd frontend
npm ci
npm run dev
```

The Vite development server runs on <http://localhost:5173> and proxies
`/api` and `/actuator` requests to a backend running on port 8080. To run the
frontend checks:

```bash
cd frontend
npm run build
npm test -- --run
```

For a full local environment, prefer the Docker Compose quick start above;
it supplies PostgreSQL, Kafka, Kafdrop, Prometheus, Grafana, the backend, and
the frontend together.

