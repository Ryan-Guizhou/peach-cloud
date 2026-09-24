---
name: grilling
description: Grill the user relentlessly about a plan, decision, or idea. Use when a complex task needs requirements, trade-offs, compatibility, migration, or implementation boundaries confirmed before work begins.
---

Interview the user relentlessly until you reach a shared understanding. Map this as a **design tree**: every decision branches into the decisions that hang off it.

Work the tree in **rounds**. The **frontier** is every decision whose prerequisites are already settled. Ask the whole frontier in one round: number each question and give your recommended answer. Then wait for the user's answers before the next round.

Format a round like:

```text
❓ Q1 - <question title>: <question body>

➡️ <recommended answer>

---

❓ Q2 - <question title>: <question body>

➡️ <recommended answer>
```

Each round reshapes the tree. Recompute the frontier after the user's answers. A question whose answer depends on another open question belongs to a later round.

Finding facts is the Agent's job, never the user's. Inspect repository files and available tools for facts that can be discovered. Decisions remain the user's.

The session is done only when the frontier is empty. Then summarize the agreed Implementation Contract and wait for explicit user confirmation before implementation.
