package com.vestisen.repository;

import com.vestisen.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("SELECT c FROM Conversation c WHERE c.annonce.id = :annonceId AND (c.buyer.id = :userId OR c.seller.id = :userId)")
    Optional<Conversation> findByAnnonceIdAndUserId(@Param("annonceId") Long annonceId, @Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.createdAt DESC")
    List<Conversation> findByBuyerIdOrSellerId(@Param("userId") Long userId);
}
