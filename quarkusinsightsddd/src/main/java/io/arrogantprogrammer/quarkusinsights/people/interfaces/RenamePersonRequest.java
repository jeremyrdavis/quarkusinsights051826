package io.arrogantprogrammer.quarkusinsights.people.interfaces;

/**
 * Request body for {@code PUT /people/{id}/name}.
 *
 * <p>JSON shape:
 * <pre>{@code
 * {"name": {"first": "Holly", "last": "Cummins"}}
 * }</pre>
 *
 * <p>Part of the People bounded context, interfaces layer.
 *
 * @param name the new name for the person
 */
public record RenamePersonRequest(NameDto name) {
}
