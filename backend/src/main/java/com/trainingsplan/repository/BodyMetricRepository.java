package com.trainingsplan.repository;

import com.trainingsplan.entity.BodyMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BodyMetricRepository extends JpaRepository<BodyMetric, Long> {

    Optional<BodyMetric> findTopByUserIdAndMetricTypeOrderByRecordedAtDesc(Long userId, String metricType);

    List<BodyMetric> findByUserIdAndMetricTypeOrderByRecordedAtDesc(Long userId, String metricType);

    List<BodyMetric> findByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<BodyMetric> findByUserIdAndMetricTypeAndSourceActivityId(Long userId, String metricType, Long sourceActivityId);

    List<BodyMetric> findByUserIdAndSourceActivityId(Long userId, Long sourceActivityId);

    @Modifying
    @Query("DELETE FROM BodyMetric b WHERE b.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT b.metricType FROM BodyMetric b WHERE b.user.id = :userId")
    List<String> findDistinctMetricTypesByUserId(@Param("userId") Long userId);

    // ── Neue Methoden für MetricsKernelService ────────────────────────────

    /** Alle Einträge eines Typs in einem Datumsbereich (für Fenster-Aggregationen wie ACWR). */
    List<BodyMetric> findByUserIdAndMetricTypeAndRecordedAtBetween(
            Long userId, String metricType, LocalDate from, LocalDate to);

    /**
     * Tagesbasierter Upsert-Lookup: findet einen Eintrag ohne sourceActivityId
     * (d.h. tagesbezogen berechnet, nicht einer Activity zugeordnet).
     */
    List<BodyMetric> findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNullOrderByIdDesc(
            Long userId, String metricType, LocalDate recordedAt);

    /**
     * Convenience-Wrapper um {@link #findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNullOrderByIdDesc}:
     * liefert den juengsten Eintrag (groesste id) als Optional. Verhindert NonUniqueResultException
     * bei (selten verbliebenen) Duplikaten.
     */
    default Optional<BodyMetric> findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
            Long userId, String metricType, LocalDate recordedAt) {
        return findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNullOrderByIdDesc(
                userId, metricType, recordedAt).stream().findFirst();
    }

    /**
     * Letzter bekannter Wert eines Typs bis einschließlich {@code date}
     * (für "Vortageswert"-Abfragen beim Backfill).
     */
    Optional<BodyMetric> findTopByUserIdAndMetricTypeAndRecordedAtLessThanEqualOrderByRecordedAtDesc(
            Long userId, String metricType, LocalDate date);
}
