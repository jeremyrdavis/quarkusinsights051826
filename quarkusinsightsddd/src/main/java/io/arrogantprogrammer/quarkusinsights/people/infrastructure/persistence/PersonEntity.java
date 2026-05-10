package io.arrogantprogrammer.quarkusinsights.people.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence model for the Person aggregate root.
 *
 * <p>Lives in the infrastructure layer — separate from the pure-POJO
 * domain {@link io.arrogantprogrammer.quarkusinsights.people.domain.Person}
 * to keep the domain free of any persistence imports. The
 * {@link PersonMapper} translates between the two.
 *
 * <p>Schema notes:
 * <ul>
 *   <li>Flat columns for nested Value Objects: {@code name_first} /
 *       {@code name_last} (PersonName), {@code email} (Email),
 *       {@code bio} (Bio), and {@code social_twitter} /
 *       {@code social_linkedin} / {@code social_website} (SocialLinks).
 *       The social columns are nullable because the links are
 *       {@link java.util.Optional} in the domain VO.</li>
 *   <li>No UNIQUE constraint on {@code email} per design intent:
 *       the same email address is allowed to appear in multiple Person
 *       records (e.g., placeholder or test data). Uniqueness is a
 *       business concern that can be enforced at the application layer
 *       if needed in a future plan.</li>
 *   <li>{@code @Version}: optimistic locking column for safe concurrent
 *       updates.</li>
 * </ul>
 *
 * <p>Extends {@link PanacheEntityBase} (not {@link io.quarkus.hibernate.orm.panache.PanacheEntity})
 * so the {@code @Id} is an explicit {@code UUID} rather than an
 * auto-generated {@code Long}.
 *
 * <p>Part of the People bounded context, infrastructure layer.
 */
@Entity
@Table(name = "person")
public class PersonEntity extends PanacheEntityBase {

    @Id
    @Column(nullable = false, updatable = false)
    public UUID id;

    @Column(name = "name_first", nullable = false, length = 100)
    public String nameFirst;

    @Column(name = "name_last", nullable = false, length = 100)
    public String nameLast;

    @Column(nullable = false, length = 320)
    public String email;

    @Column(nullable = false, length = 2000)
    public String bio;

    @Column(name = "social_twitter", nullable = true, length = 2048)
    public String socialTwitter;

    @Column(name = "social_linkedin", nullable = true, length = 2048)
    public String socialLinkedin;

    @Column(name = "social_website", nullable = true, length = 2048)
    public String socialWebsite;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @Version
    public long version;

    /**
     * Default constructor required by JPA.
     */
    public PersonEntity() {
    }
}
