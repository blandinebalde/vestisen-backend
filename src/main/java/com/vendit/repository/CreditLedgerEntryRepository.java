package com.vendit.repository;

import com.vendit.model.CreditLedgerEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditLedgerEntryRepository extends JpaRepository<CreditLedgerEntry, Long> {

    @EntityGraph(attributePaths = {"annonce", "creditTransaction"})
    List<CreditLedgerEntry> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditLedgerEntry e SET e.annonce.id = :annonceId WHERE e.id = :ledgerId AND e.annonce IS NULL")
    int attachAnnonceId(@Param("ledgerId") long ledgerId, @Param("annonceId") long annonceId);
}
