package com.trainingsplan.repository;

import com.trainingsplan.entity.AppNewsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppNewsViewRepository extends JpaRepository<AppNewsView, Long> {

    boolean existsByAppNews_IdAndUser_Id(Long newsId, Long userId);

    long countByAppNews_Id(Long newsId);

    long countByAppNews_IdAndViewedAtAfter(Long newsId, LocalDateTime after);

    /**
     * Aggregates view counts per topicTag for published news whose publishedAt is between (from, now).
     * Returns rows: [topicTag, viewCount, newsCount].
     */
    @Query("SELECT n.topicTag as tag, COUNT(v.id) as views, COUNT(DISTINCT n.id) as newsCount " +
            "FROM AppNews n LEFT JOIN AppNewsView v ON v.appNews = n " +
            "WHERE n.isPublished = true " +
            "AND n.topicTag IS NOT NULL AND LENGTH(TRIM(n.topicTag)) > 0 " +
            "AND n.publishedAt >= :from " +
            "GROUP BY n.topicTag " +
            "ORDER BY views DESC, newsCount DESC")
    List<Object[]> aggregateTrendingTags(@Param("from") LocalDateTime from);
}
