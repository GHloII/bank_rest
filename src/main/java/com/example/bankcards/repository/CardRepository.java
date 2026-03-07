package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByIdAndUserId(Long id, Long userId);

    Page<Card> findByUserId(Long userId, Pageable pageable);

    List<Card> findByUserIdAndStatus(Long userId, CardStatus status);

    Optional<Card> findByPanLast4AndUserId(String last4, Long userId);

    // soft-delete: only non-deleted cards
    @Query("select c from Card c where c.userId = :userId and c.deletedAt is null")
    Page<Card> findByUserIdAndDeletedAtIsNull(@Param("userId") Long userId, Pageable pageable);

    // User cards search with pagination (only own, non-deleted). Filters are optional.
    @Query(value = """
            SELECT * FROM cards
            WHERE (:userId IS NULL OR user_id = :userId)
              AND deleted_at IS NULL
              AND (:status IS NULL OR status = :status)
              AND (:last4 IS NULL OR pan_last4 = :last4)
              AND (:ownerName IS NULL OR lower(owner_name) LIKE lower(concat('%', cast(:ownerName as text), '%')))
            """, nativeQuery = true)
    Page<Card> searchUserCards(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("last4") String last4,
            @Param("ownerName") String ownerName,
            Pageable pageable
    );

    // for admin: all cards including deleted 
    @Query(value = "SELECT * FROM cards WHERE (:ownerName IS NULL OR lower(owner_name) LIKE lower(concat('%', cast(:ownerName as text), '%')))", nativeQuery = true)
    Page<Card> findAllByOwnerNameContaining(@Param("ownerName") String ownerName, Pageable pageable); // (admin)

    // Admin cards search with pagination. includeDeleted=true will return soft-deleted cards too.
    @Query(value = """
            SELECT * FROM cards
            WHERE (:userId IS NULL OR user_id = :userId)
              AND (:status IS NULL OR status = :status)
              AND (:last4 IS NULL OR pan_last4 = :last4)
              AND (:ownerName IS NULL OR lower(owner_name) LIKE lower(concat('%', cast(:ownerName as text), '%')))
              AND (:includeDeleted = true OR deleted_at IS NULL)
            """, nativeQuery = true)
    Page<Card> searchAdminCards(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("last4") String last4,
            @Param("ownerName") String ownerName,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable
    ); // (admin)

    Page<Card> findAll(Pageable pageable); // (admin)
}
