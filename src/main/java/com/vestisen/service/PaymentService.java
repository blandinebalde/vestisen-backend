package com.vestisen.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.vestisen.model.Annonce;
import com.vestisen.model.Payment;
import com.vestisen.model.PublicationTarif;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.PaymentRepository;
import com.vestisen.repository.PublicationTarifRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    
    public Payment createPayment(Long annonceId, User user, Payment.PaymentMethod paymentMethod) {
        Annonce annonce = annonceRepository.findById(annonceId)
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
            payment.setTransactionId("LIVRAISON-" + annonceId);
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
    
    public Payment confirmPayment(Long paymentId, User user) {
        Payment payment = paymentRepository.findById(paymentId)
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
