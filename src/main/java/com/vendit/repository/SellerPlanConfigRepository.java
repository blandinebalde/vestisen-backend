package com.vendit.repository;

import com.vendit.model.SellerPlan;
import com.vendit.model.SellerPlanConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerPlanConfigRepository extends JpaRepository<SellerPlanConfig, SellerPlan> {

    List<SellerPlanConfig> findByActiveTrueOrderByDisplayOrderAsc();
}
