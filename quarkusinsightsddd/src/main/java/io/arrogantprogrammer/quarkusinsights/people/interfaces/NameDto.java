package io.arrogantprogrammer.quarkusinsights.people.interfaces;

/**
 * Nested name object used in People REST request bodies.
 *
 * <p>Shared by {@link RegisterPersonRequest} and
 * {@link RenamePersonRequest} so the JSON shape is consistent:
 * {@code {"name": {"first": "Holly", "last": "Cummins"}}}.
 *
 * <p>Part of the People bounded context, interfaces layer.
 *
 * @param first the person's given (first) name
 * @param last  the person's family (last) name
 */
public record NameDto(String first, String last) {
}
