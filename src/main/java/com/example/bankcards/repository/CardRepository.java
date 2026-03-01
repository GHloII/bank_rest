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
    @Query("""
            select c from Card c
            where c.userId = :userId
              and c.deletedAt is null
              and (:status is null or c.status = :status)
              and (:last4 is null or c.panLast4 = :last4)
              and (:ownerName is null or lower(c.ownerName) like lower(concat('%', :ownerName, '%')))
            """)
    Page<Card> searchUserCards(
            @Param("userId") Long userId,
            @Param("status") CardStatus status,
            @Param("last4") String last4,
            @Param("ownerName") String ownerName,
            Pageable pageable
    );

    // for admin: all cards including deleted 
    @Query("select c from Card c where (:ownerName is null or lower(c.ownerName) like lower(concat('%', :ownerName, '%')))")
    Page<Card> findAllByOwnerNameContaining(@Param("ownerName") String ownerName, Pageable pageable); // (admin)

    // Admin cards search with pagination. includeDeleted=true will return soft-deleted cards too.
    @Query("""
            select c from Card c
            where (:userId is null or c.userId = :userId)
              and (:status is null or c.status = :status)
              and (:last4 is null or c.panLast4 = :last4)
              and (:ownerName is null or lower(c.ownerName) like lower(concat('%', :ownerName, '%')))
              and (:includeDeleted = true or c.deletedAt is null)
            """)
    Page<Card> searchAdminCards(
            @Param("userId") Long userId,
            @Param("status") CardStatus status,
            @Param("last4") String last4,
            @Param("ownerName") String ownerName,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable
    ); // (admin)

    Page<Card> findAll(Pageable pageable); // (admin)
}
