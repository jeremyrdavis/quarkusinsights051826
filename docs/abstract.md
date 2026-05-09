# Domain-Driven Design and Hexagonal Architecture with Quarkus (and Agentic AI)

Quarkus makes it easy to build microservices applications. It is just as productive for teams building monolithic applications. Monolithic applications took a hit because traditional architectures like MVC tended to combine framework and business logic. By combining Quarkus with Domain-Driven Design, we can build monoliths that avoid the coupling that gave "monolith" a bad name.

Twenty-two years after *Domain-Driven Design: Tackling Complexity in the Heart of Software*, Eric Evans' central idea still holds: isolate the domain, and everything else becomes a detail.

In this episode, we'll build a monolithic Quarkus application from scratch and use it to explore the fundamentals of Domain-Driven Design:

- What a Domain Model actually is (and what it is not)
- The difference between Entities and Value Objects
- How Aggregates protect business invariants
- How Services coordinate behavior without owning business rules
- Why Hexagonal Architecture makes your application extensible and isolates infrastructure code from your model

You'll see how to structure a Quarkus project so:

- Business rules live in the domain layer
- REST controllers stay thin
- Persistence and messaging remain adapters, not the core
- Your model reflects the ubiquitous language of the business

Then we'll take it one step further and unleash the agents. In an AI-assisted world, coding agents are excellent at generating controllers, repositories, and integration code. But they are pattern machines, not domain experts, and will happily distribute business rules across your codebase. When business logic lives inside Aggregates and Value Objects, agents can generate plumbing all day long without corrupting what matters.

After this session, you will be able to:

- Model a core domain in Quarkus using Entities, Value Objects, and Aggregates
- Encapsulate business rules so they don't leak into REST controllers or repositories
- Apply Hexagonal Architecture in a practical, non-academic way
- Understand why DDD makes AI-assisted development safer and more scalable
- Structure Quarkus projects so speed doesn't destroy clarity

Quarkus optimizes infrastructure. Domain-Driven Design optimizes the business.

No slides. No ivory-tower theories. Just Quarkus, code, and agentic-ready architecture.

---

## Short version

Quarkus makes it easy to build and ship applications. Domain-Driven Design optimizes business logic. In this episode, we'll build a monolithic Quarkus application using DDD and hexagonal architecture to keep our business rules clean, explicit, and protected. Then we'll introduce coding agents and show why strong domain boundaries are the key to making AI-assisted development scale. All code. No theory.
