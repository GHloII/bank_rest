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

    @Query("select t from Transaction t where t.fromCardId = :cardId or t.toCardId = :cardId order by t.createdAt desc")
    List<Transaction> findByCardIdOrderByCreatedAtDesc(@Param("cardId") Long cardId);

    @Query("select t from Transaction t where t.fromCardId = :cardId or t.toCardId = :cardId order by t.createdAt desc")
    Page<Transaction> findByCardIdOrderByCreatedAtDesc(@Param("cardId") Long cardId, Pageable pageable);

    @Query("""
            select t from Transaction t
            join Card cFrom on t.fromCardId = cFrom.id
            join Card cTo on t.toCardId = cTo.id
            where cFrom.userId = :userId or cTo.userId = :userId
            order by t.createdAt desc
            """)
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select t from Transaction t
            join Card cFrom on t.fromCardId = cFrom.id
            join Card cTo on t.toCardId = cTo.id
            where t.id = :id and (cFrom.userId = :userId or cTo.userId = :userId)
            """)
    Optional<Transaction> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<Transaction> findByStatus(TransactionStatus status);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    Page<Transaction> findAll(Pageable pageable);
}
