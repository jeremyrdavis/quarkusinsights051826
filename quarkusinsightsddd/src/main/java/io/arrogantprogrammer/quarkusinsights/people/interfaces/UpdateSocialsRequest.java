package io.arrogantprogrammer.quarkusinsights.people.interfaces;

/**
 * Request body for {@code PUT /people/{id}/socials}.
 *
 * <p>All three fields are nullable: omitting a field (or passing
 * {@code null}) clears the corresponding social link on the Person.
 *
 * <p>JSON shape:
 * <pre>{@code
 * {
 *   "twitter": "https://twitter.com/example",
 *   "linkedin": null,
 *   "website": "https://example.com"
 * }
 * }</pre>
 *
 * <p>Part of the People bounded context, interfaces layer.
 *
 * @param twitter  Twitter/X profile URI string, or null to clear
 * @param linkedin LinkedIn profile URI string, or null to clear
 * @param website  personal/professional website URI string, or null to clear
 */
public record UpdateSocialsRequest(String twitter, String linkedin, String website) {
}
