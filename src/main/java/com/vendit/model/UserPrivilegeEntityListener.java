package com.vendit.model;

import com.vendit.config.ApplicationContextProvider;
import com.vendit.security.PrivilegeSealService;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Recalcule le sceau de privilège avant chaque écriture utilisateur.
 */
public class UserPrivilegeEntityListener {

    @PrePersist
    @PreUpdate
    public void refreshPrivilegeSealBeforeSave(User user) {
        try {
            PrivilegeSealService svc = ApplicationContextProvider.getBean(PrivilegeSealService.class);
            svc.applySeal(user);
        } catch (IllegalStateException ex) {
            throw new PersistenceException("Impossible de calculer le sceau de privilège (contexte Spring indisponible)", ex);
        }
    }
}
