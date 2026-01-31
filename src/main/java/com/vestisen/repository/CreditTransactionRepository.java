package com.vestisen.repository;

import com.vestisen.model.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    List<CreditTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<CreditTransaction> findByPaymentProviderId(String paymentProviderId);
    boolean existsByCode(String code);
}
