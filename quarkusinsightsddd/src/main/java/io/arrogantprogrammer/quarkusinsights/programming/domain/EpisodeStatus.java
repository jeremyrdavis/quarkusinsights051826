package io.arrogantprogrammer.quarkusinsights.programming.domain;

/**
 * Lifecycle states of an Episode aggregate.
 *
 * <p>Episodes progress through a state machine:
 * {@link #SCHEDULED} → {@link #LIVE} → {@link #PUBLISHED}, with a
 * branching transition to {@link #CANCELED} from {@code SCHEDULED}.
 * The transitions are gated by behavior methods on {@link Episode};
 * direct status mutation is not supported. The valid transitions and
 * their preconditions are:
 *
 * <ul>
 *   <li>{@code SCHEDULED}: the initial state; reached only via
 *       {@link Episode#schedule}.</li>
 *   <li>{@code SCHEDULED → LIVE}: via {@link Episode#goLive()}, which
 *       requires the air date to be on or before the current local
 *       date.</li>
 *   <li>{@code LIVE → PUBLISHED}: via {@link Episode#publish()}, which
 *       requires an abstract, at least one presenter, and at least
 *       one speaker.</li>
 *   <li>{@code SCHEDULED → CANCELED}: via {@link Episode#cancel(String)};
 *       cancellation is only available before the episode goes live.</li>
 * </ul>
 *
 * <p>{@code PUBLISHED} and {@code CANCELED} are terminal states.
 *
 * <p>Part of the Programming bounded context, domain layer.
 */
public enum EpisodeStatus {
    /** The episode has been planned but has not yet aired. */
    SCHEDULED,
    /** The episode is currently airing or has just aired and is awaiting publication. */
    LIVE,
    /** The episode has aired and been published; visible in the public catalog. */
    PUBLISHED,
    /** The episode was cancelled before it aired and will not be published. */
    CANCELED
}
