package com.trainingsplan.repository;

import com.trainingsplan.entity.UserVo2MaxState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVo2MaxStateRepository extends JpaRepository<UserVo2MaxState, Long> {

    Optional<UserVo2MaxState> findTopByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    List<UserVo2MaxState> findByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    @Modifying
    @Query("delete from UserVo2MaxState s where s.user.id = :userId")
    void deleteByUserId(Long userId);
}
