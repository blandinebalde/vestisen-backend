package com.vendit.util;

import com.vendit.dto.PublicationTarifDTO;
import com.vendit.model.PublicationTarif;
import com.vendit.service.SellerPlanService;

public final class PublicationTarifMapper {

    private PublicationTarifMapper() {
    }

    public static PublicationTarifDTO toDto(PublicationTarif t) {
        PublicationTarifDTO dto = new PublicationTarifDTO();
        dto.setId(t.getId());
        dto.setTypeName(t.getTypeName());
        dto.setPrice(t.getPrice());
        dto.setDurationDays(t.getDurationDays());
        dto.setTopPublication(t.isTopPublication());
        dto.setActive(t.isActive());
        return dto;
    }

    public static int normalizeDurationDays(Integer durationDays) {
        if (durationDays == null || durationDays <= 0) {
            return 0;
        }
        return Math.min(durationDays, SellerPlanService.MAX_ANNONCE_LIFETIME_DAYS);
    }
}
