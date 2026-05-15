package com.vendit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vendit.model.CreditUsage;

import java.util.List;

@Repository
public interface CreditUsageRepository extends JpaRepository<CreditUsage, Long> {

    List<CreditUsage> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<CreditUsage> findByAnnonceId(Long annonceId);
}
