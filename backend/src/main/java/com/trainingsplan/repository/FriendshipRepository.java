package com.trainingsplan.repository;

import com.trainingsplan.entity.Friendship;
import com.trainingsplan.entity.FriendshipStatus;
import com.trainingsplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByRequesterAndAddressee(User requester, User addressee);

    List<Friendship> findAllByAddresseeAndStatus(User addressee, FriendshipStatus status);

    List<Friendship> findAllByRequesterAndStatus(User requester, FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE f.status = com.trainingsplan.entity.FriendshipStatus.ACCEPTED " +
            "AND (f.requester.id = :userId OR f.addressee.id = :userId) " +
            "ORDER BY f.respondedAt DESC")
    List<Friendship> findAcceptedFriendships(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE (f.requester.id = :a AND f.addressee.id = :b) " +
            "   OR (f.requester.id = :b AND f.addressee.id = :a)")
    boolean existsBetween(@Param("a") Long userA, @Param("b") Long userB);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE f.status = com.trainingsplan.entity.FriendshipStatus.ACCEPTED " +
            "AND ((f.requester.id = :a AND f.addressee.id = :b) " +
            "  OR (f.requester.id = :b AND f.addressee.id = :a))")
    boolean areAcceptedFriends(@Param("a") Long userA, @Param("b") Long userB);

    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester.id = :a AND f.addressee.id = :b) " +
            "OR (f.requester.id = :b AND f.addressee.id = :a)")
    Optional<Friendship> findBetween(@Param("a") Long userA, @Param("b") Long userB);
}
