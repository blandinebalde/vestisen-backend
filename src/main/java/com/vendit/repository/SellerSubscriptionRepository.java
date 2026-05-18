package com.vendit.repository;

import com.vendit.model.SellerSubscription;
import com.vendit.model.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SellerSubscriptionRepository extends JpaRepository<SellerSubscription, Long> {

    Optional<SellerSubscription> findByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SellerSubscription s WHERE s.user.id = :userId")
    Optional<SellerSubscription> findByUserIdForUpdate(@Param("userId") Long userId);

    List<SellerSubscription> findByStatusAndGraceUntilBefore(SubscriptionStatus status, LocalDateTime before);

    Optional<SellerSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    long countByScheduledDowngradeIsNotNull();

    @Query("SELECT s.planType, COUNT(s) FROM SellerSubscription s JOIN s.user u WHERE u.role = 'VENDEUR' GROUP BY s.planType")
    List<Object[]> countVendeurSubscriptionsByPlan();

    @Query("SELECT s.status, COUNT(s) FROM SellerSubscription s JOIN s.user u WHERE u.role = 'VENDEUR' GROUP BY s.status")
    List<Object[]> countVendeurSubscriptionsByStatus();

    @Query("SELECT COUNT(s) FROM SellerSubscription s JOIN s.user u WHERE u.role = 'VENDEUR' AND s.status = :status")
    long countVendeurByStatus(@Param("status") SubscriptionStatus status);

    @Query("SELECT COUNT(s) FROM SellerSubscription s JOIN s.user u WHERE u.role = 'VENDEUR' "
            + "AND s.planType IN ('PRO', 'PREMIUM') "
            + "AND s.status IN ('ACTIVE', 'PAST_DUE')")
    long countPaidSubscribers();
}
