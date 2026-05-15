package com.vendit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vendit.model.Category;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);

    long countByActiveTrue();

    long countByActiveFalse();

    @Query("SELECT c FROM Category c WHERE " +
           "(:active IS NULL OR c.active = :active) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Category> findAdminFiltered(
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);
}
