# gitzi-mobile

Native Android client for [gitzi](https://github.com/rhosys/gitzi) — a Kanban
agent harness for software development pipelines. Where the `gitzi` CLI/TUI
runs as a local daemon you drive from a terminal, this app is a first-class
mobile UI for the same epics/tasks/review-queue/chat model, talking to a
**deployed Gitzi backend** — there is no local daemon on mobile.

No Gitzi backend exists yet. This app was built against an assumed HTTP +
WebSocket contract (see [`docs/api-contract.md`](docs/api-contract.md)) so a
future server has a concrete target, and so the whole app is explorable today
with a built-in demo backend.

## Stack

Kotlin, Jetpack Compose, Hilt, Retrofit + OkHttp (REST) and OkHttp WebSocket
(live updates), kotlinx.serialization, DataStore for connection settings.
Built from the same Gradle/tooling template as this workspace's other Android
apps (see `scripts/`), with the BLE/wear-specific pieces stripped out.

## Screens

| Tab | Purpose |
|-----|---------|
| **Board** | All 11 Kanban columns (Prioritized → Done, including buffer columns), horizontally scrollable |
| **Epics** | Epic list + detail, create epics/tasks |
| **Review** | The human review queue — **one item at a time**, matching gitzi's core "never show two things needing attention at once" principle. Agent questions get a free-text answer; buffer approvals get Approve/Reject with feedback |
| **Chat** | Conversation with the main agent |
| **Settings** | Connection info, providers (OpenAI-compatible / Bedrock / self-hosted Ollama, discovery + activation), agent role → model/provider table, repos, and an activity log |

## Demo data vs. a real backend

Debug builds default to an in-memory demo backend (`MockGitziRepository`)
seeded with sample epics, tasks in every column, and a review queue — the
whole app works with nothing deployed. Flip **Settings → Use mock data** (or
during first-run Setup) to hot-swap to the real API at any time, no rebuild
required. Release builds always talk to the configured server; there is no
mock-data escape hatch in production.

## Development

```bash
npm run setup   # one-time: Android SDK, KVM, ktlint
npm run start   # boots the shared WorkspaceAVD emulator, builds, installs, launches, streams logs
```

`scripts/dev.sh` is the single orchestrator — see its header for details.
This app shares the `WorkspaceAVD` emulator with the other Android apps in
this workspace; don't give it its own AVD.

```bash
npm run check   # compile + lint + unit tests
```

## Project conventions

See `CLAUDE.md` for dev workflow conventions and how the "one thing at a
time" principle from the `gitzi` backend applies to this UI.
