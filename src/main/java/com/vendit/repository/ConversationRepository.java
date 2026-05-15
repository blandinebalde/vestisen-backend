package com.vendit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vendit.model.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByPublicId(UUID publicId);
    @Query("SELECT c FROM Conversation c WHERE c.annonce.id = :annonceId AND (c.buyer.id = :userId OR c.seller.id = :userId)")
    Optional<Conversation> findByAnnonceIdAndUserId(@Param("annonceId") Long annonceId, @Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findByBuyerIdOrSellerId(@Param("userId") Long userId);
}
