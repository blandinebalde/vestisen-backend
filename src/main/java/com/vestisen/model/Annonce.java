package com.vestisen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationType publicationType = PublicationType.STANDARD;
    
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
    private User seller;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    
    private int viewCount = 0;
    private int contactCount = 0;
    
    @OneToOne(mappedBy = "annonce", cascade = CascadeType.ALL)
    private Payment payment;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    
    public enum Category {
        FEMME, HOMME, ACCESSOIRE, PROMOTION
    }
    
    public enum PublicationType {
        STANDARD, PREMIUM, TOP_PUB
    }
    
    public enum Condition {
        NEUF, OCCASION, TRES_BON_ETAT, BON_ETAT
    }
    
    public enum Status {
        PENDING, APPROVED, REJECTED, SOLD, EXPIRED
    }
}
