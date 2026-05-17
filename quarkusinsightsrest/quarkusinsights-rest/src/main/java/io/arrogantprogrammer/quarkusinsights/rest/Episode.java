package io.arrogantprogrammer.quarkusinsights.rest;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.sql.Date;
import java.util.Collection;

@Entity
public class Episode extends PanacheEntity {

    private String title;
    private String description;
    private String url;
    private Date airDate;

    @ManyToMany
    @JoinTable(
            name = "episode_hosts",
            joinColumns = @JoinColumn(name = "episode_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Collection<Person> hosts;

    @ManyToMany
    @JoinTable(
            name = "episode_guests",
            joinColumns = @JoinColumn(name = "episode_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private Collection<Person> guests;

    public Episode() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getAirDate() {
        return airDate;
    }

    public void setAirDate(Date airDate) {
        this.airDate = airDate;
    }

    public Collection<Person> getHosts() {
        return hosts;
    }

    public void setHosts(Collection<Person> hosts) {
        this.hosts = hosts;
    }

    public Collection<Person> getGuests() {
        return guests;
    }

    public void setGuests(Collection<Person> guests) {
        this.guests = guests;
    }
}
