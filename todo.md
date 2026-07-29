# Gitzi Mobile — TODO

## Chat: primary interface redesign

Chat is the primary interface. Everything else (board, epics, tasks) is secondary/supportive.

### Direct messaging
- Basic back-and-forth text chat with the agent (exists today, needs polish)

### Message rendering
- Full markdown support in agent responses (headings, bold, italic, lists, blockquotes)
- Code blocks with syntax highlighting
- Inline hyperlinks that deep-link to in-app screens (tasks, epics, review queue items)
- Rich cards for attachments / documents — embedded previews in the message flow
- Plain text with auto-linked references (e.g., task IDs, epic names become tappable links)

### Conversation stacking
- On-device LLM (Android on-board) classifies each new user message as "same conversation" or "new topic"
- **Same conversation**: tell the server to stop processing the previous message, amend the context, continue in the same session
- **New topic**: push a new session onto the stack. The previous conversation continues running in the background. The new session carries forward the last 50 turns as context, with the new message as primary focus.
- Stacking is implicit/automatic — no manual "new thread" button
- The agent (backend) decides when a stacked side conversation is finished and pops the stack automatically
- UI needs to visually represent the stack (e.g., show that a background conversation is still running)

### Notifications & background behavior
- No push notifications (FCM) — the app does not poll or push data in the background
- When the user opens the app, data is fetched fresh (review queue polled every 60s, board/epics on demand)
- There will always be something waiting — no need to alert the user externally
- This keeps the app simple: no notification channels, no background services, no runtime notification permissions

### Message interactions
- Long-press on any message opens a context menu: Copy, Edit (user messages only), Delete (user messages only)
- **Copy**: activates text selection mode on the message for 20 seconds, then auto-deactivates. During copy mode, the user can select and copy parts of the message text.
- **Edit**: converts the message bubble into an inline text area with Cancel / Save buttons. On save, calls `PATCH /v1/chat/sessions/{sessionId}/messages/{messageId}` with the new content.
- **Delete**: shows a confirmation dialog. On confirm, calls `DELETE /v1/chat/sessions/{sessionId}/messages/{messageId}` and removes the message from the local chat list.
- Edit and Delete are only available on user-sent messages (ChatRole.User).

### Object detail — top-down sliding panel over chat
- Tapping a task, epic, review queue item, or any domain object opens a **top-down sliding panel** that overlays the chat:
  - The panel slides down from the top of the screen, partially covering the chat
  - User can **pull it down** to expand and review the object in full detail
  - User can **slide it back up** to minimize/dismiss and focus on the chat
  - The chat interface remains visible and **fully interactive underneath** at all times — the user can type, scroll, and send messages even with the panel partially open
- **5 snap points** — the user can position the panel at any of 5 heights to show exactly the right amount of content vs. chat:
  - **Hidden** (0%) — panel fully retracted, chat fills the screen
  - **Peek** (~20%) — shows the object title/summary
  - **Quarter** (~40%) — enough for key fields and a preview
  - **Half** (~60%) — balanced split between panel and chat
  - **Full** (~85-90%) — panel nearly covers the screen, chat peeking at the bottom
- **Content scrolls within the panel** at any snap point — the user slides the panel to set the display area, then scrolls the content inside it
- **Fling-dismiss**: fast swipe up snaps back to hidden
- **Panel content**:
  - Two display modes: **immutable** (read-only view, default) and **mutable** (edit mode)
  - Read-only items (completed tasks, agent output, code diffs) always show immutable view
  - Editable items (tasks, epics) have a toggle/button to switch to mutable view with inline editing
- The contextual chat below is scoped to the object — the agent knows what's being discussed and can act on it
- This replaces the current standalone TaskDetail and EpicDetail screens
- Same pattern applies to review queue items, code diffs, canvases brought up in chat

### Board layout
- Board shows columns vertically (top to bottom), one section per logical stage
- **Buffer stages are merged into the preceding column**, not shown as separate sections:
  - CodingBuffer tasks appear inside the Designing section
  - ReviewBuffer tasks appear inside the Coding section
  - SecurityAuditBuffer tasks appear inside the Reviewing section
  - DeploymentBuffer tasks appear inside the Auditing section
- Buffer tasks are visually distinguished with a **tint/highlight** on their card to indicate they are blocked on human review
- This reduces the number of board sections from ~10 to ~6 (Prioritized, Designing, Coding, Reviewing, Auditing, Done)
- Tapping any task card opens the top-down sliding detail panel (see above)

### Epic & task creation / editing
- **Creation**: epics and tasks are created only via the API (agent/backend). No create UI in the app — no FABs, no "new epic" dialogs.
- **Editing**: existing epics and tasks can be edited either:
  - Directly by the user in the app (via the sliding detail panel's mutable mode)
  - Via the API (agent updating fields through chat or backend logic)
- The Epics screen is a read-only overview with progress bars; tapping an epic opens the detail panel
- The Board screen is a read-only overview of task flow; tapping a task opens the detail panel

### Authentication — Authress
- Cookie/session-based auth via Authress — no bearer tokens, no API keys
- Port the Authress React Native SDK to Kotlin for this app
- Login flow handled by the Authress SDK; session cookie stored and refreshed automatically
- OkHttp `CookieJar` attaches the session cookie to every HTTP request and poll
- The app never stores or transmits raw credentials
- Default setup flow uses the default Gitzi API URL — no server URL input needed
- Server URL remains editable in Settings for self-hosted / on-prem deployments
- **Session expiry**: Authress SDK silently refreshes the session in the background
- **Refresh failure during chat send**:
  - Store the pending message locally
  - Redirect to Authress login screen
  - After re-auth, redisplay the stored message for user confirmation
  - On confirm, send with `If-Match` — if the conversation moved on, discard and notify
- **Refresh failure on reads**: show cached data, re-auth when the user takes an action

### Onboarding
- On first launch, connect to default API and run Authress login flow
- After auth, **validate the user's setup** — check account state (projects, agents, repos, config)
- Show **tailored onboarding** based on validation:
  - New user with no projects → guided setup ("Let's set up your first project")
  - Existing user with active work → jump straight to chat with a summary of what's waiting
- The agent drives onboarding conversationally in chat — no separate onboarding screens/wizards
- Setup screen (server URL entry) is only shown if the user explicitly navigates to Settings for self-hosted config

### Review queue — conversational flow
- Each review item opens as a **stacked conversation** — the agent pushes it, not the user
- The user discusses, asks questions, sees diffs/tasks via the sliding detail panel, then approves/rejects/answers conversationally
- One at a time — resolve one, agent pops it and stacks the next. No choosing, no deferring.
- **Two prominent buttons centered on screen**: go to main chat, or go to a review queue item — the user picks what they want
- **Overflow menu** includes a full list of all **active agent executions** (not just review items — everything agents are currently working on)
- The agent pops the review conversation when resolved and moves to the next item automatically

### Agent status
- Agent activity is visible **only on task cards** on the board (e.g., "agent is working" state)
- No typing indicators, status bars, or special chat UI for agent activity
- The board is the place to see what agents are doing; chat is just conversation

### Search
- No search bar anywhere in the app
- Search is conversational — ask the agent to find tasks, epics, or history

### Code diff rendering
- **Inline preview**: compact syntax-highlighted diff block in the chat message (unified format)
- **Full diff viewer**: tapping the inline preview opens the sliding detail panel with a complete diff view
- Diff viewer is **native Kotlin** — optimized diff library, not a WebView
- Supports syntax highlighting, line numbers, added/removed/context lines
- Must handle large diffs performantly (lazy rendering, virtualized scrolling)

### Attachments
- No file or media attachments in chat for now
- Users reference what they need via URLs or text references (task IDs, file paths, epic names)
- The agent resolves references from the repo/system

### Offline behavior & caching
- **All data cached locally** — board, chat history, epics, config are browsable read-only when offline
- **Message queuing**: user can compose messages offline; they queue locally and send on reconnect
- **Conditional sends**: before sending a queued message, the API validates a precondition (`If-Match` / ETag on the chat session state)
  - If the conversation is still in the same state → send normally
  - If the conversation has moved on (`412 Precondition Failed`) → discard the queued message and notify the user
- This prevents stale messages from landing in the wrong context (e.g., session was popped, agent already responded)
- **API contract**: chat mutation endpoints (`POST /v1/chat`, `PATCH .../messages/{mid}`) must support `If-Match` with session ETags

### Network model — synchronous chat + polling
- **No WebSocket, no SSE, no background services**
- **Chat responses are synchronous** — the API call blocks until the agent responds; no polling for chat
- **Review queue polling**: poll `GET /v1/review-queue` every 60 seconds while foregrounded
- Board, epics, and config are fetched on demand (screen open / pull-to-refresh), not continuously polled
- Server sends full snapshots — no incremental diff reconciliation needed
- Existing `GitziEventSocket` (WebSocket client) to be replaced with synchronous + polling approach

### Deep linking
- App registers Android App Links (`https://gitzigo.com/...`) and custom scheme (`gitzi://...`)
- Links to tasks, epics, chat sessions open directly into the sliding detail panel
- External sources (Slack, email, browser) can deep-link into specific objects

### Error handling & retries
- **Silent retries** — network failures retry automatically, user sees cached data in the meantime
- **No error banners or toast messages** for transient failures
- **API must be idempotent** — retries use `previousMessageId` / `If-Match` expectations so duplicate sends are safe
- **User error only on contention** — display an error message only when a `4XX` response indicates real contention (e.g., `412 Precondition Failed` for stale context, `409 Conflict`)
- All other failures (5XX, timeouts, network drops) are retried silently

### Theming
- Light / dark / system toggle in Settings
- Material 3 defaults for now — custom brand theme later

### Animations
- Custom, polished animations throughout — not just Material defaults
- Gesture-driven sliding detail panel animation
- Smooth, intentional transitions between screens (bottom nav, overflow items)

### Accessibility
- Fully accessible from day one
- TalkBack support with meaningful content descriptions on all interactive elements
- Minimum touch targets (48dp per Material guidelines)
- Screen reader navigation order must make sense for the chat-first layout
- Sliding detail panel must be accessible via gestures and focus navigation
- Diff viewer must support screen readers (line-by-line reading of changes)

### Platform targets
- Min API 31 (Android 12) — Material You dynamic colors; may broaden later
- All strings in `res/values/strings.xml` from day one — English authored, Google Play auto-translate for other languages

### Security
- HTTPS + Authress session cookies — no cert pinning, no encrypted storage, no integrity checks

### Testing
- Unit tests on everything — ViewModels, repositories, mappers, domain logic
- No UI or integration tests for now

### Analytics
- PostHog (already wired up) tracking crashes and feature usage
- Screen visits, board vs. chat engagement, review completion time

### Performance
- No hard budgets — measure and optimize as opportunities arise

### Settings screen
- Account info, logout
- Theme toggle (light / dark / system)
- Agent role editing (model + provider)
- Build version ref, build number, CI/CD build number
- Advanced: server URL for self-hosted / on-prem deployments
- Debug only: mock data toggle

### Chat history
- One continuous chat history the user scrolls back through
- Previous stacked conversations are not in the main history — linked from their respective tasks

### Multi-device / multi-user
- Chat is **per-user** — each person has their own conversation with the agent
- No shared team chat
- Same user on multiple devices sees the same chat (synced via the server)

### Contextual bring-up — `@` references
- Typing `@` in the chat input triggers **autocomplete** for domain objects (tasks, epics, review queue items, code diffs, canvases)
- Selecting an item from autocomplete embeds an inline chip/reference in the message
- Both user and agent can see the referenced object, comment on it, and the agent can modify it based on the discussion
- Review queue items surface in the chat — the agent presents a question or approval request in the chat, and the user responds conversationally (not through a separate form on a separate screen)
- This is also the mechanism for new messages: attach a task/epic/diff to your message via `@` reference for context

### Detail panel editing — all fields
- All fields on tasks and epics are **directly user-editable** in the sliding detail panel's mutable mode
- Tasks: title, description, status/column, priority, assignee, blocked-by — everything
- Epics: title, description, target date, linked tasks — everything
- No fields are locked behind agent-only access — the user can always edit directly
- The agent can also update fields via chat or backend logic

### On-device conversation classification
- **Gemini Nano via AICore** classifies each user message as "same conversation" or "new topic"
- Zero bundle size cost — the model is part of the OS on supported devices (Pixel 8+, select Samsung flagships, Android 14+)
- **Fallback**: devices without Gemini Nano send messages directly to the API; the server handles classification server-side
- The on-device model bridges the gap for instant local classification; the server is the authoritative fallback

### Chat input UX
- **Single-line input** that grows as the user types, up to **half the available screen height**
- Once the input reaches half-height, it becomes internally scrollable (no further vertical growth)
- **No character limit** on messages
- **Send button is always visible** (not conditionally shown)
- **Empty send = refresh/retry**: pressing send with no text refreshes the current conversation state (acts as a retry/refresh of the known state)
- Non-empty send: sends the message text as a new chat message

### Conversation stacking — visible indicators
- **Stack depth count** is visible in the UI — user can see how many conversations are stacked (e.g., "2 stacked")
- **Review queue count** is visible — user can see how many review items are waiting
- Background async processing (agent working, stacked conversations processing) is **not** visually prominent — only the counts matter
- The user knows at a glance: am I in a stacked conversation? How many review items are pending?
