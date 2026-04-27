package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Configuration entry for a LADV (Leichtathletik-Datenverarbeitung) PUBLIC API
 * import source. Each row is a separate Landesverband (LV) we want to poll for
 * stadionferne (off-stadium) running events that admins can adopt as PACR
 * competitions.
 */
@Entity
@Table(name = "ladv_import_source")
public class LadvImportSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    /** LADV Landesverbandskennzeichen, e.g. BY, WUE, HE, NI. */
    @Column(nullable = false, length = 3)
    private String lv;

    /** When true, only events flagged "bestenlistenfaehig" by LADV are imported. */
    @Column(name = "bestenlistenfaehig_only", nullable = false)
    private boolean bestenlistenfaehigOnly = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_fetched_at")
    private LocalDateTime lastFetchedAt;

    @Column(name = "last_fetch_status", length = 500)
    private String lastFetchStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLv() { return lv; }
    public void setLv(String lv) { this.lv = lv; }
    public boolean isBestenlistenfaehigOnly() { return bestenlistenfaehigOnly; }
    public void setBestenlistenfaehigOnly(boolean v) { this.bestenlistenfaehigOnly = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(LocalDateTime v) { this.lastFetchedAt = v; }
    public String getLastFetchStatus() { return lastFetchStatus; }
    public void setLastFetchStatus(String v) { this.lastFetchStatus = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
