package com.trainingsplan.repository;

import com.trainingsplan.entity.AppNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppNewsRepository extends JpaRepository<AppNews, Long> {

    List<AppNews> findAllByOrderByCreatedAtDesc();

    List<AppNews> findAllByIsPublishedTrueOrderByPublishedAtDesc();

    /** True if a news item with the given external GUID already exists (dedup check). */
    boolean existsByExternalGuid(String externalGuid);

    /** Counts published news referencing a specific external source (used before deleting a source). */
    long countByExternalSource_Id(Long externalSourceId);

    /**
     * Published news matching the given language preferences.
     * Items with a NULL language (legacy / manual) are always included so that
     * manually-authored news stay visible regardless of user preferences.
     */
    @Query("SELECT n FROM AppNews n WHERE n.isPublished = true "
            + "AND (n.language IS NULL OR n.language IN :langs) "
            + "ORDER BY n.publishedAt DESC")
    List<AppNews> findPublishedForLanguages(@Param("langs") List<String> langs);
}
