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

### Contextual bring-up
- User can pull a domain object (task, epic, review queue item, code diff, canvas) into the active chat as a rich inline element
- Both user and agent can see it, comment on it, and the agent can modify it based on the discussion
- Review queue items surface in the chat — the agent presents a question or approval request in the chat, and the user responds conversationally (not through a separate form on a separate screen)
- This is also the mechanism for new messages: attach a task/epic/diff to your message for context
