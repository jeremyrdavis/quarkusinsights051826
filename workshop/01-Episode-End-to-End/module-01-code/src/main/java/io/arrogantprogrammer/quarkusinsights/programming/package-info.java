/**
 * Programming bounded context — <strong>you will build this</strong> over the
 * ten steps of Module 01.
 *
 * <p>By the end of the workshop, this package will contain the Episode
 * aggregate (with an inside-aggregate Abstract entity), the episode lifecycle
 * (SCHEDULED → LIVE → PUBLISHED), and presenter/speaker assignment. This
 * context is the source of truth for what shows exist and what state they
 * are in.
 *
 * <p>Sub-packages (currently empty — you'll fill them in):
 * <ul>
 *   <li>{@code domain} — value objects, events, exceptions, the {@code Episode}
 *       aggregate, and the {@code EpisodeRepository} port (steps 1–7)</li>
 *   <li>{@code application} — commands and {@code EpisodeService} (steps 7–8)</li>
 *   <li>{@code infrastructure.persistence} — Panache entity, mapper, and
 *       repository implementation (step 9)</li>
 *   <li>{@code interfaces} — REST resource, request/response DTOs, and
 *       exception mappers (step 10)</li>
 * </ul>
 *
 * <p>See {@code workshop/01-Episode-End-to-End/README.md} for the full
 * curriculum.
 */
package io.arrogantprogrammer.quarkusinsights.programming;
