

package com.vendit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vendit.model.CreditConfig;

@Repository
public interface CreditConfigRepository extends JpaRepository<CreditConfig, Long> {
}
