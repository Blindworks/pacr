package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cycle_entries")
public class CycleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "physical_symptoms", length = 500)
    private String physicalSymptoms;

    @Column(name = "mood", length = 50)
    private String mood;

    @Column(name = "energy_level")
    private Integer energyLevel;

    @Column(name = "sleep_hours")
    private Integer sleepHours;

    @Column(name = "sleep_minutes")
    private Integer sleepMinutes;

    @Column(name = "sleep_quality", length = 20)
    private String sleepQuality;

    @Column(name = "flow_intensity", length = 20)
    private String flowIntensity;

    @Column(name = "notes", length = 1000)
    private String notes;

    public CycleEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public String getPhysicalSymptoms() { return physicalSymptoms; }
    public void setPhysicalSymptoms(String physicalSymptoms) { this.physicalSymptoms = physicalSymptoms; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public Integer getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(Integer energyLevel) { this.energyLevel = energyLevel; }

    public Integer getSleepHours() { return sleepHours; }
    public void setSleepHours(Integer sleepHours) { this.sleepHours = sleepHours; }

    public Integer getSleepMinutes() { return sleepMinutes; }
    public void setSleepMinutes(Integer sleepMinutes) { this.sleepMinutes = sleepMinutes; }

    public String getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(String sleepQuality) { this.sleepQuality = sleepQuality; }

    public String getFlowIntensity() { return flowIntensity; }
    public void setFlowIntensity(String flowIntensity) { this.flowIntensity = flowIntensity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
