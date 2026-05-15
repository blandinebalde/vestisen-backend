
package com.vendit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vendit.model.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndAnnonceId(Long userId, Long annonceId);

    boolean existsByUserIdAndAnnonceId(Long userId, Long annonceId);

    void deleteByAnnonce_Id(Long annonceId);
}
