package com.vestisen.repository;

import com.vestisen.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndAnnonceId(Long userId, Long annonceId);

    boolean existsByUserIdAndAnnonceId(Long userId, Long annonceId);
}
