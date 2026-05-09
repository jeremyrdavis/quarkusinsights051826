# Quarkus Insights — DDD + Hexagonal Architecture + Agentic AI

Reference repository for the QuarkusInsights episode
**Domain-Driven Design and Hexagonal Architecture with Quarkus (and Agentic AI)**.

This repo serves two co-equal purposes:

1. **A reference architecture** for building monolithic Quarkus applications using
   Domain-Driven Design and Hexagonal Architecture. Clone it, study it, copy it.
   Useful even if you have no interest in AI-assisted development.
2. **Proof that this architecture is a strong fit for agentic development.**
   Four CLI coding agents independently built the same application from
   [`SPEC.md`](SPEC.md) in parallel sandboxes — proof points that the architecture
   generalizes across CLIs, not a competition.

## Run the reference app

```bash
cd quarkusinsightsddd
./mvnw quarkus:dev
# Open http://localhost:8080
```

Requires Docker (for Dev Services Postgres) and a JDK with a Java 25 toolchain.

## What's in this repo

| File | What it is |
|---|---|
| [`SPEC.md`](SPEC.md) | The specification handed to four CLI coding agents |
| [`SHOW_NOTES.md`](SHOW_NOTES.md) | Stream timeline, presentation notes, architectural checklist |
| [`docs/abstract.md`](docs/abstract.md) | The talk's abstract |
| `quarkusinsightsddd/` | The reference application |

## The four agent outputs

Each agent ran in a Docker Sandbox in YOLO mode, given only `SPEC.md`:

| Agent | Branch |
|---|---|
| Claude Code | [`agent/claude-code`](../../tree/agent/claude-code) |
| Codex | [`agent/codex`](../../tree/agent/codex) |
| Gemini CLI | [`agent/gemini`](../../tree/agent/gemini) |
| Copilot CLI | [`agent/copilot`](../../tree/agent/copilot) |

Compare any branch against the reference:

```bash
git diff main agent/claude-code -- 'quarkusinsightsddd/src/**/Episode*.java'
```

## Replay the live-coding refactor

The naive-CRUD starting point is on `live/start`; the destination is on `live/end`.
The six refactor steps are six commits between them:

```bash
git log --oneline live/start..live/end
```

## License

Apache 2.0 — see [`LICENSE`](LICENSE).
