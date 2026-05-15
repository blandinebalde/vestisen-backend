package com.vendit.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import com.vendit.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Verrou pessimiste pour débit crédits / solde (évite course entre deux créations d'annonces).
     * À utiliser uniquement dans une transaction active.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
    Optional<User> findByPublicId(UUID publicId);

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    @Query("SELECT u FROM User u WHERE u.email = :value OR u.phone = :value")
    Optional<User> findByEmailOrPhone(@Param("value") String value);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByCode(String code);
    
    @Query("SELECT u FROM User u WHERE u.verificationToken = :token")
    Optional<User> findByVerificationToken(@Param("token") String token);
    
    @Query("SELECT u FROM User u WHERE u.resetPasswordToken = :token")
    Optional<User> findByResetPasswordToken(@Param("token") String token);

    @Query("SELECT u FROM User u WHERE u.privilegeSeal IS NULL OR u.privilegeSeal = ''")
    List<User> findUsersWithMissingPrivilegeSeal();

    @Query("SELECT u.role, COUNT(u) FROM User u GROUP BY u.role")
    List<Object[]> countByRole();

    long countByEnabledFalse();

    long countByEmailVerifiedFalse();

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(u.code IS NOT NULL AND LOWER(u.code) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findAdminFiltered(
            @Param("role") User.Role role,
            @Param("enabled") Boolean enabled,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.planGraceUntil IS NOT NULL AND u.planGraceUntil < :before")
    List<User> findByRoleAndPlanGraceUntilBefore(
            @Param("role") User.Role role,
            @Param("before") LocalDateTime before);
}
