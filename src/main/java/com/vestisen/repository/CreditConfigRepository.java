package com.vestisen.repository;

import com.vestisen.model.CreditConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditConfigRepository extends JpaRepository<CreditConfig, Long> {
}
