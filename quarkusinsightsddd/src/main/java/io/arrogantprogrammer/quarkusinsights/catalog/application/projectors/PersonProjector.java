package io.arrogantprogrammer.quarkusinsights.catalog.application.projectors;

import io.arrogantprogrammer.quarkusinsights.catalog.infrastructure.persistence.PublicEpisodeViewEntity;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonBioUpdated;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRenamed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Event projector that keeps the embedded presenter and speaker summaries in
 * {@code public_episode_view} consistent with changes in the People bounded
 * context.
 *
 * <p>Part of the Public Catalog bounded context, application layer.
 *
 * <p>Subscribed events and their projector actions:
 * <ul>
 *   <li>{@link PersonRenamed} — loads all {@link PublicEpisodeViewEntity} rows
 *       that contain the renamed person in either their {@code presenters} or
 *       {@code speakers} embedded collections, and updates the {@code displayName}
 *       field in each matching embeddable. Because a person may appear in many
 *       episodes, this operation is O(n) in the number of episodes where the
 *       person has been assigned. For the demo scale this is acceptable; a
 *       production implementation would add a database index on the embedded
 *       {@code person_id} column or use a separate denormalized name table.</li>
 *   <li>{@link PersonBioUpdated} — intentional no-op. The catalog view does not
 *       include a bio field; biographical text is out of scope for the episode
 *       listing page. This observer exists only to document the intentional
 *       decision not to react to bio updates.</li>
 * </ul>
 */
@ApplicationScoped
public class PersonProjector {

    /**
     * Handles {@link PersonRenamed}: cascades the new name into every view row
     * that contains the person as a presenter or speaker.
     *
     * @param event the rename event; never null (CDI guarantees this)
     */
    @Transactional
    public void onPersonRenamed(@Observes PersonRenamed event) {
        UUID personId = event.personId().value();
        String newDisplayName = event.newName().displayName();

        // Load all view entities (catalog scale is small; a production system
        // would query only rows referencing this personId via a secondary index).
        List<PublicEpisodeViewEntity> allViews = PublicEpisodeViewEntity.listAll();

        for (PublicEpisodeViewEntity view : allViews) {
            boolean changed = false;

            for (var presenter : view.presenters) {
                if (presenter.personId.equals(personId)) {
                    presenter.displayName = newDisplayName;
                    changed = true;
                }
            }

            for (var speaker : view.speakers) {
                if (speaker.personId.equals(personId)) {
                    speaker.displayName = newDisplayName;
                    changed = true;
                }
            }

            if (changed) {
                view.lastUpdatedAt = event.occurredAt();
            }
        }
    }

    /**
     * Handles {@link PersonBioUpdated}: intentional no-op.
     *
     * <p>The Public Catalog view ({@code public_episode_view}) does not include
     * a biography field. Bio text belongs on a dedicated person-profile page,
     * not on the episode listing. Subscribing to this event with a no-op body
     * documents the intentional decision to ignore it, preventing future
     * developers from wondering whether this projector simply missed it.
     *
     * @param event the bio-update event (ignored)
     */
    public void onPersonBioUpdated(@Observes PersonBioUpdated event) {
        // Intentional no-op: bio is not projected into the episode view.
    }
}
