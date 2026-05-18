package com.vendit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vendit.model.Annonce;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {

    Optional<Annonce> findByPublicId(UUID publicId);

    /**
     * Verrou pessimiste (SELECT … FOR UPDATE) pour l’achat : un seul acheteur peut finaliser
     * tant que la transaction n’est pas terminée.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Annonce a WHERE a.publicId = :publicId")
    Optional<Annonce> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    Optional<Annonce> findByCode(String code);

    boolean existsByCode(String code);
    
    Page<Annonce> findByStatus(Annonce.Status status, Pageable pageable);
    
    Page<Annonce> findByCategory_IdAndStatus(Long categoryId, Annonce.Status status, Pageable pageable);
    
    Page<Annonce> findBySellerIdAndStatus(Long sellerId, Annonce.Status status, Pageable pageable);
    
    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("SELECT a FROM Annonce a WHERE a.seller.id = :sellerId")
    Page<Annonce> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("SELECT a FROM Annonce a WHERE a.seller.id = :sellerId AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:search IS NULL OR " +
           "LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(a.code IS NOT NULL AND LOWER(a.code) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Annonce> findBySellerIdFiltered(
            @Param("sellerId") Long sellerId,
            @Param("status") Annonce.Status status,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(a.viewCount), 0), COALESCE(SUM(a.contactCount), 0) FROM Annonce a WHERE a.seller.id = :sellerId")
    Object[] sumViewsAndContactsForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT a.status, COUNT(a) FROM Annonce a WHERE a.seller.id = :sellerId GROUP BY a.status")
    List<Object[]> countBySellerIdGroupByStatus(@Param("sellerId") Long sellerId);

    @EntityGraph(attributePaths = {"category", "seller"})
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

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Annonce> findByPublicationTypeAndStatusOrderByCreatedAtDesc(
            String publicationType,
            Annonce.Status status,
            Pageable pageable);

    /** Annonces approuvées dont la durée de publication est dépassée (expiresAt &lt; now). Paginé pour éviter tout chargement massif. */
    Page<Annonce> findByStatusAndExpiresAtNotNullAndExpiresAtBefore(Annonce.Status status, LocalDateTime date, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("SELECT a FROM Annonce a WHERE a.status = 'APPROVED' ORDER BY a.viewCount DESC")
    List<Annonce> findTopViewedAnnonces(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("SELECT a FROM Annonce a WHERE a.status = 'APPROVED' ORDER BY a.contactCount DESC")
    List<Annonce> findTopContactedAnnonces(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "seller"})
    List<Annonce> findByBuyer_IdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    /** Compte les annonces par seller_id pour une liste d'ids (évite N+1 et chargement lazy). */
    @Query("SELECT a.seller.id, COUNT(a) FROM Annonce a WHERE a.seller.id IN :sellerIds GROUP BY a.seller.id")
    List<Object[]> countBySellerIdIn(@Param("sellerIds") List<Long> sellerIds);

    long countBySeller_Id(Long sellerId);

    @Query("SELECT COUNT(a) FROM Annonce a WHERE a.seller.id = :sellerId AND a.status IN ('PENDING', 'APPROVED') AND a.planPaused = false")
    long countActivePublicationsBySeller(@Param("sellerId") Long sellerId);

    /** Somme des crédits dépensés (publicationCreditCost sur toutes les annonces). */
    @Query("SELECT COALESCE(SUM(a.publicationCreditCost), 0) FROM Annonce a")
    BigDecimal sumCreditsSpent();

    @Query(value = "SELECT YEAR(created_at) AS y, MONTH(created_at) AS m, COALESCE(SUM(publication_credit_cost), 0) AS total " +
            "FROM annonces WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "GROUP BY YEAR(created_at), MONTH(created_at) ORDER BY y, m", nativeQuery = true)
    List<Object[]> sumCreditsSpentByMonth();

    @Query(value = "SELECT YEAR(created_at) AS y, COALESCE(SUM(publication_credit_cost), 0) AS total " +
            "FROM annonces WHERE YEAR(created_at) >= YEAR(CURDATE()) - 4 " +
            "GROUP BY YEAR(created_at) ORDER BY y", nativeQuery = true)
    List<Object[]> sumCreditsSpentByYear();

    @Query(value = "SELECT YEAR(created_at) AS y, MONTH(created_at) AS m, status, COUNT(*) AS cnt " +
            "FROM annonces WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "GROUP BY YEAR(created_at), MONTH(created_at), status ORDER BY y, m", nativeQuery = true)
    List<Object[]> countAnnoncesByMonthAndStatus();

    @Query(value = "SELECT YEAR(created_at) AS y, status, COUNT(*) AS cnt FROM annonces " +
            "WHERE YEAR(created_at) >= YEAR(CURDATE()) - 4 GROUP BY YEAR(created_at), status ORDER BY y", nativeQuery = true)
    List<Object[]> countAnnoncesByYearAndStatus();

    @Query("SELECT a.category.id, a.category.name, COUNT(a) FROM Annonce a GROUP BY a.category.id, a.category.name ORDER BY COUNT(a) DESC")
    List<Object[]> countAnnoncesByCategory();

    @Query("SELECT a.status, COUNT(a) FROM Annonce a GROUP BY a.status")
    List<Object[]> countAnnoncesByStatus();

    @Query("SELECT COUNT(a), COALESCE(SUM(a.viewCount), 0), COALESCE(SUM(a.contactCount), 0) FROM Annonce a")
    List<Object[]> sumViewsAndContacts();

    @Query("SELECT COUNT(a) FROM Annonce a WHERE a.status = :status AND a.createdAt < :before")
    long countByStatusAndCreatedAtBefore(@Param("status") Annonce.Status status, @Param("before") LocalDateTime before);

    @Query("SELECT COUNT(a) FROM Annonce a WHERE a.createdAt >= :since")
    long countByCreatedAtSince(@Param("since") LocalDateTime since);

    @EntityGraph(attributePaths = {"seller", "category"})
    @Query("SELECT a FROM Annonce a WHERE a.status = :status ORDER BY a.createdAt ASC")
    List<Annonce> findByStatusOrderByCreatedAtAsc(@Param("status") Annonce.Status status, Pageable pageable);

    @Query("SELECT COUNT(a), COALESCE(SUM(a.viewCount), 0), COALESCE(SUM(a.contactCount), 0) FROM Annonce a WHERE a.status = 'APPROVED'")
    List<Object[]> sumViewsAndContactsApproved();

    @EntityGraph(attributePaths = {"category", "seller"})
    @Query("SELECT a FROM Annonce a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(a.code IS NOT NULL AND LOWER(a.code) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "LOWER(a.seller.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CONCAT(COALESCE(a.seller.firstName, ''), ' ', COALESCE(a.seller.lastName, ''))) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.category.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(a.location, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Annonce> findAdminFiltered(
            @Param("status") Annonce.Status status,
            @Param("search") String search,
            Pageable pageable);
}
