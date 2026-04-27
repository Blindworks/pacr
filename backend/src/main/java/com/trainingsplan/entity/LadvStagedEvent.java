package com.trainingsplan.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One row per event fetched from LADV via {@link LadvImportSource}. Admins
 * decide per-row whether to adopt this event as a {@link Competition} (status
 * IMPORTED) or skip it (IGNORED). Re-fetching the same source dedupes via the
 * unique (source_id, ladv_id) constraint.
 */
@Entity
@Table(name = "ladv_staged_event",
       uniqueConstraints = @UniqueConstraint(name = "uk_ladv_staged_event_source_ladv",
                                             columnNames = {"source_id", "ladv_id"}))
public class LadvStagedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private LadvImportSource source;

    /** LADV-internal numeric ID of the event ("id" field in the JSON response). */
    @Column(name = "ladv_id", nullable = false)
    private Long ladvId;

    @Column(name = "veranstaltungsnummer", length = 40)
    private String veranstaltungsnummer;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(length = 255)
    private String kategorie;

    @Column(length = 255)
    private String veranstalter;

    @Column(length = 500)
    private String homepage;

    @Column(length = 255)
    private String ort;

    @Column(length = 15)
    private String plz;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private boolean abgesagt = false;

    @Lob
    @Column(name = "raw_json", nullable = false, columnDefinition = "LONGTEXT")
    private String rawJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private LadvStagedEventStatus status = LadvStagedEventStatus.NEW;

    /** Set when status flips to IMPORTED — links to the created competition. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_competition_id")
    private Competition importedCompetition;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private User importedBy;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LadvImportSource getSource() { return source; }
    public void setSource(LadvImportSource source) { this.source = source; }
    public Long getLadvId() { return ladvId; }
    public void setLadvId(Long ladvId) { this.ladvId = ladvId; }
    public String getVeranstaltungsnummer() { return veranstaltungsnummer; }
    public void setVeranstaltungsnummer(String v) { this.veranstaltungsnummer = v; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime v) { this.startTime = v; }
    public String getKategorie() { return kategorie; }
    public void setKategorie(String v) { this.kategorie = v; }
    public String getVeranstalter() { return veranstalter; }
    public void setVeranstalter(String v) { this.veranstalter = v; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String v) { this.homepage = v; }
    public String getOrt() { return ort; }
    public void setOrt(String v) { this.ort = v; }
    public String getPlz() { return plz; }
    public void setPlz(String v) { this.plz = v; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double v) { this.latitude = v; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double v) { this.longitude = v; }
    public boolean isAbgesagt() { return abgesagt; }
    public void setAbgesagt(boolean v) { this.abgesagt = v; }
    public String getRawJson() { return rawJson; }
    public void setRawJson(String v) { this.rawJson = v; }
    public LadvStagedEventStatus getStatus() { return status; }
    public void setStatus(LadvStagedEventStatus v) { this.status = v; }
    public Competition getImportedCompetition() { return importedCompetition; }
    public void setImportedCompetition(Competition v) { this.importedCompetition = v; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime v) { this.importedAt = v; }
    public User getImportedBy() { return importedBy; }
    public void setImportedBy(User v) { this.importedBy = v; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime v) { this.fetchedAt = v; }
}
