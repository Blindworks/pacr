package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "body_measurements")
public class BodyMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "measured_at", nullable = false)
    private LocalDate measuredAt;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "fat_percentage")
    private Double fatPercentage;

    @Column(name = "water_percentage")
    private Double waterPercentage;

    @Column(name = "muscle_mass_kg")
    private Double muscleMassKg;

    @Column(name = "bone_mass_kg")
    private Double boneMassKg;

    @Column(name = "visceral_fat_level")
    private Integer visceralFatLevel;

    @Column(name = "metabolic_age")
    private Integer metabolicAge;

    @Column(name = "bmi")
    private Double bmi;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL";

    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    public BodyMeasurement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(LocalDate measuredAt) { this.measuredAt = measuredAt; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getFatPercentage() { return fatPercentage; }
    public void setFatPercentage(Double fatPercentage) { this.fatPercentage = fatPercentage; }

    public Double getWaterPercentage() { return waterPercentage; }
    public void setWaterPercentage(Double waterPercentage) { this.waterPercentage = waterPercentage; }

    public Double getMuscleMassKg() { return muscleMassKg; }
    public void setMuscleMassKg(Double muscleMassKg) { this.muscleMassKg = muscleMassKg; }

    public Double getBoneMassKg() { return boneMassKg; }
    public void setBoneMassKg(Double boneMassKg) { this.boneMassKg = boneMassKg; }

    public Integer getVisceralFatLevel() { return visceralFatLevel; }
    public void setVisceralFatLevel(Integer visceralFatLevel) { this.visceralFatLevel = visceralFatLevel; }

    public Integer getMetabolicAge() { return metabolicAge; }
    public void setMetabolicAge(Integer metabolicAge) { this.metabolicAge = metabolicAge; }

    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}
