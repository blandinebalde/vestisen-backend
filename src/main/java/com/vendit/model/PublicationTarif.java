package com.vendit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "publication_tarifs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PublicationTarif {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Nom du type de publication saisi par l'admin (ex: Standard, Premium, Top Pub) */
    @Column(nullable = false, unique = true, length = 100)
    private String typeName;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    /** Durée en jours ; null ou 0 = durée max plateforme (365 j) à l'approbation */
    @Column(name = "duration_days")
    private Integer durationDays;

    /** Type « top publication » : consomme un boost abonnement si paiement SUBSCRIPTION */
    @Column(name = "top_publication", nullable = false)
    private boolean topPublication = false;
    
    private boolean active = true;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
