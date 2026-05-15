package com.vendit.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Une transaction par table : une erreur (table absente, colonne manquante) ne marque pas rollback-only
 * sur le reste du démarrage de l'application.
 */
@Service
public class PublicIdBackfillService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int backfillPublicIds(String table, String uuidExpr) {
        return entityManager.createNativeQuery(
                "UPDATE " + table + " SET public_id = " + uuidExpr + " WHERE public_id IS NULL"
        ).executeUpdate();
    }
}
