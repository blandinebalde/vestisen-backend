package com.vestisen.repository;

import com.vestisen.model.Annonce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    Optional<Annonce> findByCode(String code);

    boolean existsByCode(String code);
    
    Page<Annonce> findByStatus(Annonce.Status status, Pageable pageable);
    
    Page<Annonce> findByCategory_IdAndStatus(Long categoryId, Annonce.Status status, Pageable pageable);
    
    Page<Annonce> findBySellerIdAndStatus(Long sellerId, Annonce.Status status, Pageable pageable);
    
    @Query("SELECT a FROM Annonce a WHERE a.seller.id = :sellerId")
    Page<Annonce> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
    
    @Query("SELECT a FROM Annonce a WHERE a.status = :status AND " +
           "(:categoryId IS NULL OR a.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR a.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR a.price <= :maxPrice) AND " +
           "(:size IS NULL OR a.size = :size) AND " +
           "(:brand IS NULL OR LOWER(a.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) AND " +
           "(:condition IS NULL OR a.condition = :condition) AND " +
           "(:toutDoitPartir IS NULL OR a.toutDoitPartir = :toutDoitPartir) AND " +
           "(:latMin IS NULL OR (a.latitude IS NOT NULL AND a.latitude >= :latMin AND a.latitude <= :latMax AND a.longitude >= :lngMin AND a.longitude <= :lngMax)) AND " +
           "(:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Annonce> searchAnnonces(
        @Param("status") Annonce.Status status,
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("size") String size,
        @Param("brand") String brand,
        @Param("condition") Annonce.Condition condition,
        @Param("search") String search,
        @Param("toutDoitPartir") Boolean toutDoitPartir,
        @Param("latMin") Double latMin,
        @Param("latMax") Double latMax,
        @Param("lngMin") Double lngMin,
        @Param("lngMax") Double lngMax,
        Pageable pageable
    );
    
    List<Annonce> findByPublicationTypeAndStatusOrderByCreatedAtDesc(
        String publicationType,
        Annonce.Status status
    );

    /** Annonces approuvées dont la durée de publication est dépassée (expiresAt &lt; now). */
    List<Annonce> findByStatusAndExpiresAtNotNullAndExpiresAtBefore(Annonce.Status status, LocalDateTime date);
    
    @Query("SELECT a FROM Annonce a WHERE a.status = 'APPROVED' ORDER BY a.viewCount DESC")
    List<Annonce> findTopViewedAnnonces(Pageable pageable);
    
    @Query("SELECT a FROM Annonce a WHERE a.status = 'APPROVED' ORDER BY a.contactCount DESC")
    List<Annonce> findTopContactedAnnonces(Pageable pageable);

    List<Annonce> findByBuyer_IdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    /** Compte les annonces par seller_id pour une liste d'ids (évite N+1 et chargement lazy). */
    @Query("SELECT a.seller.id, COUNT(a) FROM Annonce a WHERE a.seller.id IN :sellerIds GROUP BY a.seller.id")
    List<Object[]> countBySellerIdIn(@Param("sellerIds") List<Long> sellerIds);

    long countBySeller_Id(Long sellerId);
}
