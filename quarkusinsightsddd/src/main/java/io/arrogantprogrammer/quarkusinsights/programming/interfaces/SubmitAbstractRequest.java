package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

/**
 * Wire DTO for {@code POST /episodes/{id}/abstract}.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 *
 * @param text the abstract body (100..5000 chars; validated by AbstractText)
 */
public record SubmitAbstractRequest(String text) {
}
