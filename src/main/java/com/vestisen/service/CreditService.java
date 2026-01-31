package com.vestisen.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.vestisen.model.CreditConfig;
import com.vestisen.model.CreditTransaction;
import com.vestisen.model.User;
import com.vestisen.repository.CreditConfigRepository;
import com.vestisen.repository.CreditTransactionRepository;
import com.vestisen.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CreditService {

    @Autowired
    private CreditConfigRepository creditConfigRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    /** Retourne la config crédits (prix FCFA par crédit). Crée une config par défaut si absente. */
    public CreditConfig getOrCreateConfig() {
        List<CreditConfig> all = creditConfigRepository.findAll();
        if (all.isEmpty()) {
            CreditConfig config = new CreditConfig();
            config.setPricePerCreditFcfa(new BigDecimal("100"));
            return creditConfigRepository.save(config);
        }
        return all.get(0);
    }

    public BigDecimal getPricePerCreditFcfa() {
        return getOrCreateConfig().getPricePerCreditFcfa();
    }

    public BigDecimal getBalance(User user) {
        User u = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        return u.getCreditBalance() != null ? u.getCreditBalance() : BigDecimal.ZERO;
    }

    /**
     * Initie un achat de crédits : crée une transaction PENDING et (si Stripe configuré) un PaymentIntent.
     * Retourne clientSecret pour Stripe ou transactionId pour confirmation manuelle.
     * Pour l'instant les achats passent : le front peut appeler confirm après création (simulation).
     * Intégration paiement réel (Stripe, Wave, Orange Money) à brancher plus tard (webhooks / redirect).
     */
    public CreditTransaction purchaseCredits(User user, BigDecimal credits, String paymentMethod) {
        if (credits == null || credits.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Credits must be positive");
        }
        CreditConfig config = getOrCreateConfig();
        BigDecimal amountFcfa = config.getPricePerCreditFcfa().multiply(credits).setScale(2, RoundingMode.HALF_UP);

        CreditTransaction tx = new CreditTransaction();
        tx.setCode(generateUniqueTransactionCode());
        tx.setUser(user);
        tx.setAmountFcfa(amountFcfa);
        tx.setCreditsAdded(credits);
        tx.setPaymentMethod(CreditTransaction.PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        tx.setStatus(CreditTransaction.Status.PENDING);
        tx.setTransactionId("CRED-" + System.currentTimeMillis() + "-" + user.getId());

        if ("STRIPE".equalsIgnoreCase(paymentMethod) && stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            try {
                Stripe.apiKey = stripeSecretKey;
                long amountCents = amountFcfa.multiply(new BigDecimal("100")).longValue();
                if (amountCents < 100) amountCents = 100;
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency("xof")
                        .addPaymentMethodType("card")
                        .putMetadata("creditTransactionId", "")
                        .build();
                PaymentIntent intent = PaymentIntent.create(params);
                tx.setPaymentProviderId(intent.getId());
                tx.setTransactionId(intent.getClientSecret());
            } catch (StripeException e) {
                throw new RuntimeException("Stripe payment creation failed", e);
            }
        }
        // WAVE, ORANGE_MONEY : à intégrer plus tard (lien paiement + webhook/callback pour confirmer)
        return creditTransactionRepository.save(tx);
    }

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateUniqueTransactionCode() {
        StringBuilder sb = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        String code = sb.toString();
        if (creditTransactionRepository.existsByCode(code)) {
            return generateUniqueTransactionCode();
        }
        return code;
    }

    public List<CreditTransaction> getTransactionsByUserId(Long userId) {
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100));
    }

    /**
     * Confirme un achat de crédits (après paiement réussi Stripe/webhook ou manuel).
     * Ajoute les crédits au solde utilisateur et marque la transaction COMPLETED.
     */
    public CreditTransaction confirmCreditPurchase(Long transactionId) {
        CreditTransaction tx = creditTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Credit transaction not found"));
        if (tx.getStatus() == CreditTransaction.Status.COMPLETED) {
            throw new RuntimeException("Transaction already completed");
        }
        User user = userRepository.findById(tx.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        BigDecimal current = user.getCreditBalance() != null ? user.getCreditBalance() : BigDecimal.ZERO;
        user.setCreditBalance(current.add(tx.getCreditsAdded()));
        userRepository.save(user);
        tx.setStatus(CreditTransaction.Status.COMPLETED);
        tx.setPaidAt(LocalDateTime.now());
        return creditTransactionRepository.save(tx);
    }

    /** Confirmation par paymentProviderId (ex: Stripe PaymentIntent id) pour webhook */
    public Optional<CreditTransaction> findByPaymentProviderId(String paymentProviderId) {
        return creditTransactionRepository.findByPaymentProviderId(paymentProviderId);
    }

    public Optional<CreditTransaction> findTransactionById(Long id) {
        return creditTransactionRepository.findById(id);
    }

    public void deductCredits(User user, BigDecimal credits) {
        User u = userRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        BigDecimal current = u.getCreditBalance() != null ? u.getCreditBalance() : BigDecimal.ZERO;
        if (current.compareTo(credits) < 0) {
            throw new RuntimeException("Solde de crédits insuffisant. Solde: " + current + ", requis: " + credits);
        }
        u.setCreditBalance(current.subtract(credits));
        userRepository.save(u);
    }
}
