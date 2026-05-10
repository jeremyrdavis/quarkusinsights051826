package io.arrogantprogrammer.quarkusinsights.people.interfaces;

/**
 * Request body for {@code PUT /people/{id}/bio}.
 *
 * <p>JSON shape: {@code {"text": "..."}}
 *
 * <p>Part of the People bounded context, interfaces layer.
 *
 * @param text the new biography text (50–2000 characters)
 */
public record UpdateBioRequest(String text) {
}
