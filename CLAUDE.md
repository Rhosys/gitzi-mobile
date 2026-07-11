# gitzi-mobile — Claude Code guidelines

## Core principle: one thing at a time

Inherited from the `gitzi` backend (see its CLAUDE.md): the user is never
shown more than one thing requiring their attention at once. This is why the
Review screen (`ui/review/`) only ever renders `queue.first()` plus a "N more
waiting" count — never a list of pending items. Any new "needs human
attention" surface must follow the same pattern: single item, act on it,
move to the next.

## No local backend

This app never runs a daemon locally. Every screen is backed by
`domain/repository/GitziRepository`, which has exactly two implementations:

- `data/remote/RemoteGitziRepository` — the real (assumed) HTTP + WebSocket
  API. See `docs/api-contract.md` for the wire contract.
- `data/mock/MockGitziRepository` (debug source set only) — in-memory demo
  data, so the app is explorable with nothing deployed.

`data/SwitchableGitziRepository` (debug only) hot-swaps between the two at
runtime based on the "Use mock data" setting. Release builds bind
`RemoteGitziRepository` directly — never add a way to fall back to mock data
in a release build.

When adding a new domain concept, mirror its shape from the `gitzi` Rust
source (`model/`, `dispatcher/`) exactly — field names, enum variants, and
ordering rules (e.g. `HumanReviewQueue`'s sort key) should match precisely so
a real backend implementation is a drop-in.

## Local development — run on the emulator

```bash
npm run start            # debug variant: boots emulator, builds, installs, launches, streams crash logs
npm run start:release    # release variant: same loop on the R8/ProGuard build — catches stripping crashes
```

`scripts/dev.sh` is the single orchestrator: runs `setup.sh` if the SDK is
missing, creates the shared `WorkspaceAVD` (android-35, pixel_7) if absent,
boots it, then gradle install + launch. This app shares one `WorkspaceAVD`
and system image with the other Android apps in this workspace — do not give
it its own AVD name.

Emulator-only helpers: `npm run emulator:create|start|delete`. KVM is
required (Linux). Troubleshooting lives in `scripts/setup.sh`.

## Repository layout quick-reference

```
docs/api-contract.md                        ← the assumed backend contract — update if you change any DTO
app/src/main/.../domain/model/              ← mirrors of the gitzi Rust models — keep field-for-field in sync
app/src/main/.../data/remote/               ← Retrofit service, DTOs, mappers, WebSocket client
app/src/debug/.../data/mock/                ← in-memory demo backend (debug builds only)
app/src/debug/.../data/SwitchableGitziRepository.kt  ← runtime mock/real hot-swap (debug builds only)
app/src/release/.../di/RepositoryModule.kt  ← release binds the real backend directly, no mock escape hatch
```
