/**
 * Engagement bounded context: the Comment and Rating aggregates. Both refer
 * to {@link io.arrogantprogrammer.quarkusinsights.shared.EpisodeId} but
 * never hold a reference to an Episode aggregate. The "one Rating per
 * (EpisodeId, AuthorHandle)" invariant is enforced in three places: the
 * SubmitRatingUseCase, a database UNIQUE constraint, and the repository
 * adapter's exception translation. See SPEC.md §3.6.
 *
 * <p>Sub-packages: {@code domain}, {@code application},
 * {@code infrastructure}, {@code interfaces}.
 */
package io.arrogantprogrammer.quarkusinsights.engagement;
