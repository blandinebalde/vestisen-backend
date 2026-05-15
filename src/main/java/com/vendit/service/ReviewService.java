package com.vendit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vendit.dto.ReviewCreateRequest;
import com.vendit.dto.ReviewDTO;
import com.vendit.model.Annonce;
import com.vendit.model.Review;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.ReviewRepository;
import com.vendit.repository.UserRepository;

import java.util.List;
import java.util.UUID;
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
        Annonce annonce = annonceRepository.findByPublicId(request.getAnnoncePublicId())
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

    public List<ReviewDTO> findByRevieweePublicId(UUID revieweePublicId, int limit) {
        User reviewee = userRepository.findByPublicId(revieweePublicId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(reviewee.getId(), PageRequest.of(0, limit))
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ReviewDTO toDTO(Review r) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(r.getId());
        dto.setAnnoncePublicId(r.getAnnonce().getPublicId());
        dto.setReviewerPublicId(r.getReviewer().getPublicId());
        dto.setReviewerName(r.getReviewer().getFirstName() + " " + r.getReviewer().getLastName());
        dto.setRevieweePublicId(r.getReviewee().getPublicId());
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
