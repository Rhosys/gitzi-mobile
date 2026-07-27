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
- When the user opens the app, the WebSocket reconnects and delivers whatever is waiting (new messages, review queue items, task updates)
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
- OkHttp `CookieJar` attaches the session cookie to every HTTP request and WebSocket upgrade
- The app never stores or transmits raw credentials
- Default setup flow uses the default Gitzi API URL — no server URL input needed
- Server URL remains editable in Settings for self-hosted / on-prem deployments

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

### Search
- No search bar anywhere in the app
- Search is conversational — ask the agent to find tasks, epics, or history

### Contextual bring-up
- User can pull a domain object (task, epic, review queue item, code diff, canvas) into the active chat as a rich inline element
- Both user and agent can see it, comment on it, and the agent can modify it based on the discussion
- Review queue items surface in the chat — the agent presents a question or approval request in the chat, and the user responds conversationally (not through a separate form on a separate screen)
- This is also the mechanism for new messages: attach a task/epic/diff to your message for context
