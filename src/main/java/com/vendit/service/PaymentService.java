package com.vendit.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.vendit.dto.PaymentResponse;
import com.vendit.model.Annonce;
import com.vendit.model.Payment;
import com.vendit.model.PublicationTarif;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.PaymentRepository;
import com.vendit.repository.PublicationTarifRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private AnnonceRepository annonceRepository;
    
    @Autowired
    private PublicationTarifRepository tarifRepository;
    
    @Value("${stripe.secret-key}")
    private String stripeSecretKey;
    
    public Payment createPayment(UUID annoncePublicId, User user, Payment.PaymentMethod paymentMethod) {
        Annonce annonce = annonceRepository.findByPublicId(annoncePublicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        
        // Vérifier que l'annonce appartient à l'utilisateur
        if (!annonce.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Annonce does not belong to user");
        }
        
        // Vérifier qu'il n'y a pas déjà un paiement pour cette annonce
        if (annonce.getPayment() != null) {
            throw new RuntimeException("Payment already exists for this annonce");
        }
        
        PublicationTarif tarif = tarifRepository.findByTypeNameAndActiveTrue(annonce.getPublicationType())
                .orElseThrow(() -> new RuntimeException("Tarif not found for type: " + annonce.getPublicationType()));
        
        Payment payment = new Payment();
        payment.setAnnonce(annonce);
        payment.setUser(user);
        payment.setAmount(tarif.getPrice());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        
        // Traitement selon la méthode de paiement
        if (paymentMethod == Payment.PaymentMethod.PAIEMENT_LIVRAISON) {
            // Paiement à la livraison : pas d'appel Stripe, statut PENDING jusqu'à confirmation manuelle
            payment.setTransactionId("LIVRAISON-" + annonce.getPublicId());
        } else if (paymentMethod == Payment.PaymentMethod.STRIPE) {
            try {
                Stripe.apiKey = stripeSecretKey;
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount((long)(tarif.getPrice().doubleValue() * 100)) // Convertir en centimes
                        .setCurrency("xof")
                        .addPaymentMethodType("card")
                        .build();
                
                PaymentIntent paymentIntent = PaymentIntent.create(params);
                payment.setPaymentProviderId(paymentIntent.getId());
                payment.setTransactionId(paymentIntent.getClientSecret());
            } catch (StripeException e) {
                throw new RuntimeException("Stripe payment creation failed", e);
            }
        }
        // ORANGE_MONEY, WAVE : à brancher selon les APIs
        
        return paymentRepository.save(payment);
    }

    public PaymentResponse toPaymentResponse(Payment p) {
        return new PaymentResponse(
                p.getPublicId(),
                p.getAnnonce().getPublicId(),
                p.getAmount(),
                p.getPaymentMethod().name(),
                p.getStatus().name(),
                p.getTransactionId(),
                p.getPaymentProviderId(),
                p.getCreatedAt(),
                p.getPaidAt());
    }
    
    public Payment confirmPayment(UUID paymentPublicId, User user) {
        Payment payment = paymentRepository.findByPublicId(paymentPublicId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: you can only confirm your own payment");
        }
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Payment already completed");
        }
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        
        // Approuver l'annonce après paiement
        Annonce annonce = payment.getAnnonce();
        annonce.setStatus(Annonce.Status.APPROVED);
        annonce.setPublishedAt(LocalDateTime.now());
        annonceRepository.save(annonce);
        
        return paymentRepository.save(payment);
    }
}
