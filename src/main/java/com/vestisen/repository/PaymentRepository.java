package com.vestisen.repository;

import com.vestisen.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByAnnonceId(Long annonceId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
}
