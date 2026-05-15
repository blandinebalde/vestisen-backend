package com.vendit.repository;

import com.vendit.model.SaleCommission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleCommissionRepository extends JpaRepository<SaleCommission, Long> {

    Optional<SaleCommission> findByPublicId(UUID publicId);

    boolean existsByAnnonce_Id(Long annonceId);

    Optional<SaleCommission> findByAnnonce_Id(Long annonceId);

    Page<SaleCommission> findBySeller_IdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);
}
