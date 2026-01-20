package com.vestisen.repository;

import com.vestisen.model.Annonce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AnnonceRepository extends JpaRepository<Annonce, Long> {
    
    Page<Annonce> findByStatus(Annonce.Status status, Pageable pageable);
    
    Page<Annonce> findByCategoryAndStatus(Annonce.Category category, Annonce.Status status, Pageable pageable);
    
    Page<Annonce> findBySellerIdAndStatus(Long sellerId, Annonce.Status status, Pageable pageable);
    
    @Query("SELECT a FROM Annonce a WHERE a.seller.id = :sellerId")
    Page<Annonce> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
    
    @Query("SELECT a FROM Annonce a WHERE a.status = :status AND " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:minPrice IS NULL OR a.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR a.price <= :maxPrice) AND " +
           "(:size IS NULL OR a.size = :size) AND " +
           "(:brand IS NULL OR LOWER(a.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) AND " +
           "(:condition IS NULL OR a.condition = :condition) AND " +
           "(:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Annonce> searchAnnonces(
        @Param("status") Annonce.Status status,
        @Param("category") Annonce.Category category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("size") String size,
        @Param("brand") String brand,
        @Param("condition") Annonce.Condition condition,
        @Param("search") String search,
        Pageable pageable
    );
    
    List<Annonce> findByPublicationTypeAndStatusOrderByCreatedAtDesc(
        Annonce.PublicationType publicationType, 
        Annonce.Status status
    );
    
    @Query("SELECT a FROM Annonce a WHERE a.status = 'APPROVED' ORDER BY a.viewCount DESC")
    List<Annonce> findTopViewedAnnonces(Pageable pageable);
    
    @Query("SELECT a FROM Annonce a WHERE a.status = 'APPROVED' ORDER BY a.contactCount DESC")
    List<Annonce> findTopContactedAnnonces(Pageable pageable);
}
