# Gitzi Mobile — TODO

## Chat: primary interface redesign

Chat is the primary interface. Everything else (board, epics, tasks) is secondary/supportive.

### Direct messaging
- Basic back-and-forth text chat with the agent (exists today, needs polish)

### Conversation stacking
- On-device LLM (Android on-board) classifies each new user message as "same conversation" or "new topic"
- **Same conversation**: tell the server to stop processing the previous message, amend the context, continue in the same session
- **New topic**: push a new session onto the stack. The previous conversation continues running in the background. The new session carries forward the last 50 turns as context, with the new message as primary focus.
- Stacking is implicit/automatic — no manual "new thread" button
- The agent (backend) decides when a stacked side conversation is finished and pops the stack automatically
- UI needs to visually represent the stack (e.g., show that a background conversation is still running)

### Contextual bring-up
- User can pull a domain object (task, epic, review queue item, code diff, canvas) into the active chat as a rich inline element
- Both user and agent can see it, comment on it, and the agent can modify it based on the discussion
- Review queue items surface in the chat — the agent presents a question or approval request in the chat, and the user responds conversationally (not through a separate form on a separate screen)
- This is also the mechanism for new messages: attach a task/epic/diff to your message for context
