package com.vestisen.repository;

import com.vestisen.model.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId, Pageable pageable);
    List<Review> findByAnnonceId(Long annonceId);
    boolean existsByAnnonceIdAndReviewerId(Long annonceId, Long reviewerId);
}
