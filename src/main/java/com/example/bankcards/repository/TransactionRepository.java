package com.example.bankcards.repository;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // transaction history for a specific card 
    @Query("select t from Transaction t where t.fromCardId = :cardId or t.toCardId = :cardId order by t.createdAt desc")
    List<Transaction> findByCardIdOrderByCreatedAtDesc(@Param("cardId") Long cardId);

    // paginated transaction history for a card
    @Query("select t from Transaction t where t.fromCardId = :cardId or t.toCardId = :cardId order by t.createdAt desc")
    Page<Transaction> findByCardIdOrderByCreatedAtDesc(@Param("cardId") Long cardId, Pageable pageable);

    // transaction history for a user 
    @Query("""
            select t from Transaction t
            join Card cFrom on t.fromCardId = cFrom.id
            join Card cTo on t.toCardId = cTo.id
            where cFrom.userId = :userId or cTo.userId = :userId
            order by t.createdAt desc
            """)
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // check that transaction belongs to user 
    @Query("""
            select t from Transaction t
            join Card cFrom on t.fromCardId = cFrom.id
            join Card cTo on t.toCardId = cTo.id
            where t.id = :id and (cFrom.userId = :userId or cTo.userId = :userId)
            """)
    Optional<Transaction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // transactions with given status 
    List<Transaction> findByStatus(TransactionStatus status);

    // transactions with given status (admin)
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable); // (admin)

    // transactions with given type (admin)
    Page<Transaction> findByType(TransactionType type, Pageable pageable); // (admin)

    // All transactions 
    Page<Transaction> findAll(Pageable pageable); // (admin)
}
