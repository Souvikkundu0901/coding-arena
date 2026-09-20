package com.codingarena.friendship.repository;

import com.codingarena.friendship.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END FROM Friendship f " +
           "WHERE ((f.requester.id = :user1 AND f.addressee.id = :user2) " +
           "   OR (f.requester.id = :user2 AND f.addressee.id = :user1)) " +
           "  AND f.status IN ('PENDING', 'ACCEPTED')")
    boolean existsPendingOrAcceptedFriendshipBetween(@Param("user1") UUID user1, @Param("user2") UUID user2);

    @Query("SELECT f FROM Friendship f WHERE f.addressee.id = :addresseeId AND f.status = 'PENDING' ORDER BY f.createdAt DESC")
    List<Friendship> findPendingIncomingByAddresseeId(@Param("addresseeId") UUID addresseeId);

    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = :userId OR f.addressee.id = :userId) AND f.status = 'ACCEPTED' ORDER BY f.respondedAt DESC, f.createdAt DESC")
    List<Friendship> findAcceptedFriendshipsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "UPDATE friendships SET status = 'ACCEPTED', responded_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'PENDING'", nativeQuery = true)
    int acceptFriendshipAtomically(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE friendships SET status = 'DECLINED', responded_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'PENDING'", nativeQuery = true)
    int declineFriendshipAtomically(@Param("id") UUID id);
}
