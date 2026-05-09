/**
 * Public Catalog bounded context: the read side. Maintains a denormalized
 * {@code PublicEpisodeView} populated by event projectors that subscribe
 * to events from Programming, People, and Engagement. Owns the public-
 * facing UI (Qute templates + HTMX endpoints for posting comments and
 * ratings); writes are delegated to the Engagement context's use cases.
 *
 * <p>Sub-packages: {@code domain}, {@code application},
 * {@code infrastructure}, {@code interfaces}.
 */
package io.arrogantprogrammer.quarkusinsights.catalog;
