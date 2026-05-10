package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import java.util.UUID;

/**
 * Wire DTO for {@code POST /episodes/{id}/presenters} and
 * {@code POST /episodes/{id}/speakers}. Carries the {@code PersonId}
 * to assign in role-agnostic form (the role is implied by the URL).
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 *
 * @param personId the Person being assigned (validated when wrapped in PersonId)
 */
public record AssignPersonRequest(UUID personId) {
}
