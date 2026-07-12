# Gitzi backend API contract (assumed)

No Gitzi server exists yet — this app was built against the contract below so
a future backend has a concrete target to implement, and so the mobile app's
Retrofit/WebSocket layer (`data/remote/`) can be pointed at a real deployment
by changing nothing but the base URL and token in Settings.

The daemon in the `gitzi` repo already exposes this exact functionality to
agents over MCP (`src/mcp/tools.rs`, `gitzi_*` tools). This contract is the
same set of operations, fronted by HTTP/WebSocket for a human operator on a
phone instead of an agent over JSON-RPC.

## Deployment model

There is **no local daemon on mobile**. The app only ever talks to a deployed
Gitzi server over HTTPS/WSS. That server is expected to support, per
operator:

- **Bring-your-own model** — the operator's own OpenAI-compatible endpoint
  and API key.
- **AWS Bedrock** — via SSO/IAM credentials resolved server-side.
- **Self-hosted** — an Ollama/vLLM box the operator runs themselves (e.g. on
  an EC2 instance), reached as an OpenAI-compatible endpoint.
- **On-prem Gitzi** — the whole daemon running inside the operator's network,
  fronted by whatever reverse proxy/URL they choose to expose to this app.

The mobile app never talks to a model provider directly — see
`domain/model/Provider.kt` for the provider shape it renders in Settings.

## Auth

Every request carries `Authorization: Bearer <token>`. Token issuance /
pairing flow (e.g. a `gitzi mobile pair` command generating a scoped token)
is left to the backend; the app just stores whatever token the user enters
in Settings and sends it on every request and on the WebSocket upgrade.

## REST — `GitziApiService` (`data/remote/GitziApiService.kt`)

| Method & path                          | Purpose                                   | Maps to `gitzi_*` MCP tool |
|-----------------------------------------|--------------------------------------------|----------------------------|
| `GET /v1/epics`                         | List epics                                  | `gitzi_list_epics`         |
| `POST /v1/epics`                        | Create epic                                 | —                          |
| `GET /v1/tasks?epic_id=`                | List tasks, optionally scoped to an epic    | `gitzi_list_tasks`         |
| `POST /v1/tasks`                        | Create task                                 | `gitzi_create_task`        |
| `PATCH /v1/tasks/{id}`                  | Update title/description                    | `gitzi_update_task`        |
| `POST /v1/tasks/{id}/park`              | Park with a reason                          | `gitzi_park_task`          |
| `POST /v1/tasks/{id}/block`             | Set `blocked_by`                            | `gitzi_block_task`         |
| `GET /v1/review-queue`                  | Full ordered queue                          | (queue projection)         |
| `POST /v1/review-queue/{id}/answer`     | Answer an agent question                    | —                          |
| `POST /v1/review-queue/{id}/approve`    | Approve a buffer item                       | —                          |
| `POST /v1/review-queue/{id}/reject`     | Reject a buffer item with feedback          | —                          |
| `GET /v1/chat` / `POST /v1/chat`        | Main-agent chat history / send a message    | —                          |
| `GET /v1/config` / `PUT /v1/config`     | Providers, agents, WIP limits, repos        | —                          |
| `POST /v1/providers/discover`           | Scan for LM Studio / Ollama / Bedrock (SSO) | provider auto-discovery    |
| `POST /v1/providers/{name}/activate`    | Turn a discovered provider on               | `gitzi_activate_provider`  |
| `GET /v1/ping`                          | Connectivity check used by Setup            | —                          |

Request/response DTOs live in `data/remote/dto/Dtos.kt`; `Mappers.kt` converts
them to/from the domain models in `domain/model/`.

## WebSocket — `GET /v1/events` (upgraded)

One connection per session, reconnected automatically whenever the server URL
or token changes (`RemoteGitziRepository`). Every message is a **full
snapshot** of one entity type — the same "rebuild the projection, don't diff
it" approach the daemon already uses for `KanbanBoard`:

```json
{"type": "tasks", "tasks": [...]}
{"type": "epics", "epics": [...]}
{"type": "review_queue", "items": [...]}
{"type": "chat", "messages": [...]}
{"type": "config", "config": {...}}
```

## The one-thing-at-a-time contract

`GET /v1/review-queue` returns the whole queue (used for the "N more
waiting" count), but the client only ever *acts* on index 0 — see
`domain/repository/GitziRepository.kt` and the Review screen. A backend
implementation should preserve the daemon's `HumanReviewQueue` ordering
exactly: agent questions before buffer approvals, questions FIFO, approvals
by rightmost column then task priority. `MockGitziRepository` replicates this
ordering client-side so demo mode and a real backend behave identically.
