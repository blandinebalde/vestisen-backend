package com.vendit.config;

import com.vendit.model.SellerPlanCatalog;
import com.vendit.model.SellerPlanDefinition;
import com.vendit.repository.SellerPlanConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SellerPlanCatalogBridge {

    private final SellerPlanConfigRepository sellerPlanConfigRepository;

    public SellerPlanCatalogBridge(SellerPlanConfigRepository sellerPlanConfigRepository) {
        this.sellerPlanConfigRepository = sellerPlanConfigRepository;
    }

    @PostConstruct
    void init() {
        SellerPlanCatalog.setLoader(() -> sellerPlanConfigRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(c -> c.toDefinition())
                .collect(Collectors.toList()));
    }
}
