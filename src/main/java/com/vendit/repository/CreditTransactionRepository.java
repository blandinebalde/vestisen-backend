package com.vendit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vendit.model.CreditTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    Optional<CreditTransaction> findByPublicId(UUID publicId);

    List<CreditTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<CreditTransaction> findByPaymentProviderId(String paymentProviderId);
    boolean existsByCode(String code);

    /** Somme des crédits ajoutés (achats confirmés). */
    @Query("SELECT COALESCE(SUM(t.creditsAdded), 0) FROM CreditTransaction t WHERE t.status = 'COMPLETED'")
    BigDecimal sumCreditsPurchased();

    @Query(value = "SELECT YEAR(created_at) AS y, MONTH(created_at) AS m, COALESCE(SUM(credits_added), 0) AS total " +
            "FROM credit_transactions WHERE status = 'COMPLETED' AND created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "GROUP BY YEAR(created_at), MONTH(created_at) ORDER BY y, m", nativeQuery = true)
    List<Object[]> sumCreditsPurchasedByMonth();

    @Query(value = "SELECT YEAR(created_at) AS y, COALESCE(SUM(credits_added), 0) AS total " +
            "FROM credit_transactions WHERE status = 'COMPLETED' AND YEAR(created_at) >= YEAR(CURDATE()) - 4 " +
            "GROUP BY YEAR(created_at) ORDER BY y", nativeQuery = true)
    List<Object[]> sumCreditsPurchasedByYear();

    @Query(value = "SELECT user_id, COALESCE(SUM(credits_added), 0) AS total FROM credit_transactions " +
            "WHERE status = 'COMPLETED' GROUP BY user_id ORDER BY total DESC LIMIT 10", nativeQuery = true)
    List<Object[]> sumCreditsPurchasedByUserTop10();

    @Query("SELECT COALESCE(SUM(t.amountFcfa), 0) FROM CreditTransaction t WHERE t.status = 'COMPLETED'")
    BigDecimal sumRevenueFcfaCompleted();

    @Query("SELECT COALESCE(SUM(t.amountFcfa), 0) FROM CreditTransaction t WHERE t.status = 'COMPLETED' AND t.createdAt >= :since")
    BigDecimal sumRevenueFcfaCompletedSince(@Param("since") LocalDateTime since);

    long countByStatus(CreditTransaction.Status status);
}
