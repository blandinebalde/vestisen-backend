package com.vestisen.service;

import com.vestisen.dto.ReviewCreateRequest;
import com.vestisen.dto.ReviewDTO;
import com.vestisen.model.Annonce;
import com.vestisen.model.Review;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.ReviewRepository;
import com.vestisen.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private AnnonceRepository annonceRepository;
    @Autowired
    private UserRepository userRepository;

    public ReviewDTO create(ReviewCreateRequest request, User reviewer) {
        Annonce annonce = annonceRepository.findById(request.getAnnonceId())
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        if (annonce.getSeller().getId().equals(reviewer.getId())) {
            throw new RuntimeException("You cannot review your own annonce");
        }
        if (annonce.getBuyer() == null || !annonce.getBuyer().getId().equals(reviewer.getId())) {
            throw new RuntimeException("Only the buyer of this annonce can leave a review");
        }
        if (reviewRepository.existsByAnnonceIdAndReviewerId(annonce.getId(), reviewer.getId())) {
            throw new RuntimeException("You have already reviewed this annonce");
        }
        Review review = new Review();
        review.setAnnonce(annonce);
        review.setReviewer(reviewer);
        review.setReviewee(annonce.getSeller());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return toDTO(reviewRepository.save(review));
    }

    public List<ReviewDTO> findByRevieweeId(Long revieweeId, int limit) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(revieweeId, PageRequest.of(0, limit))
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ReviewDTO toDTO(Review r) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(r.getId());
        dto.setAnnonceId(r.getAnnonce().getId());
        dto.setReviewerId(r.getReviewer().getId());
        dto.setReviewerName(r.getReviewer().getFirstName() + " " + r.getReviewer().getLastName());
        dto.setRevieweeId(r.getReviewee().getId());
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
