# QuarkusInsights DDD Workshop

A hands-on, code-along workshop that teaches Domain-Driven Design and Hexagonal Architecture by building a Quarkus monolith from scratch — one aggregate, ten steps, three and a half hours.

The workshop accompanies the QuarkusInsights talk and reference app at <https://github.com/jeremyrdavis/QuarkusInsights> (or the parent directory of this file). The talk explains the architecture; the reference app shows the finished thing. This workshop is the middle piece — you build it yourself.

> Quarkus optimizes infrastructure. Domain-Driven Design optimizes the business. In this workshop, we'll build a monolithic Quarkus application using DDD and hexagonal architecture to keep our business rules clean, explicit, and protected. No slides. No ivory-tower theories. Just Quarkus, code, and agentic-ready architecture.

## Who this is for

- Software developers who want to move beyond CRUD applications
- Architects looking to implement DDD in practice
- Technical leads responsible for system design decisions

**No prior DDD experience required.** You should be comfortable reading Java and have a working sense of REST APIs and relational databases.

## What you'll build

The `Episode` aggregate from the QuarkusInsights podcast domain — the source of truth for one show in the program. Episodes are scheduled, have abstracts submitted, presenters and speakers assigned, go live, and get published (or canceled). Every state transition enforces invariants. Every behavior records a domain event.

By the end you'll have:

- Six value objects (`EpisodeNumber`, `EpisodeTitle`, `AbstractId`, `AbstractText`, `AirDate`, plus `EpisodeStatus`)
- Seven domain events
- Eight typed domain exceptions
- A complete `Episode` aggregate with seven behavior methods
- An `EpisodeRepository` port + `EpisodeService` application service
- Panache + PostgreSQL persistence with explicit domain-to-entity mapping
- A JAX-RS REST adapter with exception mappers and structured error responses
- ~130 tests, most running in milliseconds without booting Quarkus

## Time budget

| Step | Topic | Time |
|---|---|---|
| 1 | Value Objects | 20 min |
| 2 | Domain Events | 15 min |
| 3 | Domain Exceptions | 20 min |
| 4 | Aggregate Skeleton | 30 min |
| 5 | Behaviors I — Content | 25 min |
| 6 | Behaviors II — Lifecycle | 25 min |
| 7 | Repository Port + Commands | 15 min |
| 8 | Application Service | 30 min |
| 9 | Persistence Adapter | 30 min |
| 10 | REST Adapter | 30 min |
| **Total** | | **~3.5 hours** |

A natural break point is after Step 5 or Step 6. Plan for a 15-minute coffee break around the 90-minute mark.

## Setup

Pick one:

- **[Local setup](Quarkus-Local.md)** — JDK 25, Maven (bundled `./mvnw`), Docker
- **[GitHub Codespaces](GitHub-Codespaces.md)** — browser-only, no install

Either path ends at the same prompt: `./mvnw test` should pass with a single `HealthCheckSmokeTest` going green in the starter project. That's how you know your environment is ready.

## How to use this workshop

Each step in the module has a paired starter and solution project:

- `01-Episode-End-to-End/module-01-code/` — the **starter**. The shared kernel is built; the Programming context is empty (TODO stubs and `package-info.java` only).
- `01-Episode-End-to-End/module-01-solution/` — the **solution**. The finished Programming context. Use it as an answer key when you get stuck.

For each step:

1. Open the step's markdown file (`01-Value-Objects.md`, `02-Domain-Events.md`, …)
2. Read the **Why This Matters** section before typing
3. Write the code shown in the **Implementation** section into your starter project
4. Write the tests from the **Testing** section
5. Run `./mvnw test` and confirm you see what **You should now see** says you should
6. If anything's broken, diff against `module-01-solution/`

The TL;DR at the top of each step is the fast-path version — read it if you're short on time and just want the core of the step.

## Module index

- [Module 01: Episode End-to-End](01-Episode-End-to-End/README.md) — the full ten-step build

(Currently the only module. Future modules might add People, Engagement, or the Catalog read model. For now: see the full QuarkusInsights reference app at `../quarkusinsightsddd/` to see what those look like in finished form.)

## What's intentionally out of scope

To fit the half-day budget, this workshop covers the **Programming** bounded context only. The full reference app at `../quarkusinsightsddd/` additionally includes:

- **People** context (Person aggregate, registration, bio/socials)
- **Engagement** context (Comments, Ratings, the "three-place rule" for uniqueness)
- **Catalog** context (CQRS read model, three event projectors)
- **Qute + HTMX UI** (landing page, episode detail with comments and ratings)
- **Native build** and production-deploy paths

Those are great to explore *after* this workshop. The patterns transfer directly.

## License

Apache 2.0 — see the top-level LICENSE file.
