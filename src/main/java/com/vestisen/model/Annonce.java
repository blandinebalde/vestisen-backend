package com.vestisen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "annonces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Annonce {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code unique de l'annonce (18 caractères alphanumériques). Généré à la création. */
    @Column(unique = true, length = 18)
    private String code;

    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    /** Nom du type de publication (référence au typeName de PublicationTarif) */
    @Column(nullable = false, length = 100)
    private String publicationType = "Standard";
    
    @Enumerated(EnumType.STRING)
    private Condition condition;
    
    private String size;
    private String brand;
    private String color;
    private String location;
    
    @ElementCollection
    @CollectionTable(name = "annonce_images", joinColumns = @JoinColumn(name = "annonce_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User seller;

    /** Acheteur lorsque l'annonce est vendue (historique d'achat du client). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User buyer;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    
    private int viewCount = 0;
    private int contactCount = 0;
    
    @OneToOne(mappedBy = "annonce", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payment payment;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    
    /** Option "tout doit partir" : prix réduits / lots */
    private boolean toutDoitPartir = false;
    /** Prix d'origine affiché si toutDoitPartir (barré) */
    private java.math.BigDecimal originalPrice;
    /** Lot (plusieurs articles vendus ensemble) */
    private boolean isLot = false;
    
    /** Paiement à la livraison accepté par le vendeur */
    private boolean acceptPaymentOnDelivery = false;
    
    /** Géolocalisation pour "vente proche de chez toi" */
    private Double latitude;
    private Double longitude;
    
    public enum Condition {
        NEUF, OCCASION, TRES_BON_ETAT, BON_ETAT
    }
    
    public enum Status {
        PENDING, APPROVED, REJECTED, SOLD, EXPIRED
    }
}
