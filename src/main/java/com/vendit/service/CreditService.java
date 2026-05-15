package com.vendit.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.vendit.dto.CreditLedgerEntryDTO;
import com.vendit.model.CreditConfig;
import com.vendit.model.CreditLedgerEntry;
import com.vendit.model.CreditLedgerMovementType;
import com.vendit.model.CreditTransaction;
import com.vendit.model.User;
import com.vendit.repository.CreditConfigRepository;
import com.vendit.repository.CreditLedgerEntryRepository;
import com.vendit.repository.CreditTransactionRepository;
import com.vendit.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CreditService {

    @Autowired
    private CreditConfigRepository creditConfigRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private CreditLedgerEntryRepository creditLedgerEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    /**
     * Stripe n'est appelé que si une vraie clé secrète est présente (évite 500 avec le placeholder du projet).
     */
    private boolean isStripeSecretConfigured() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            return false;
        }
        String k = stripeSecretKey.trim();
        if (!k.startsWith("sk_test_") && !k.startsWith("sk_live_")) {
            return false;
        }
        if (k.length() < 40) {
            return false;
        }
        String lower = k.toLowerCase();
        if (lower.contains("your_stripe") || lower.contains("placeholder") || lower.contains("example")) {
            return false;
        }
        return true;
    }

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

        if ("STRIPE".equalsIgnoreCase(paymentMethod) && isStripeSecretConfigured()) {
            try {
                Stripe.apiKey = stripeSecretKey.trim();
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
                throw new RuntimeException(
                        "Paiement Stripe indisponible : vérifiez la clé secrète (STRIPE_SECRET_KEY / stripe.secret-key). "
                                + e.getMessage(), e);
            }
        }
        // STRIPE sans clé réelle : même flux que WAVE/ORANGE (transaction PENDING + confirm manuel / simu).
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

    public List<CreditLedgerEntryDTO> getLedgerForUserId(Long userId) {
        return creditLedgerEntryRepository
                .findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100))
                .stream()
                .map(this::toLedgerDto)
                .collect(Collectors.toList());
    }

    private CreditLedgerEntryDTO toLedgerDto(CreditLedgerEntry e) {
        UUID annoncePublicId = e.getAnnonce() != null ? e.getAnnonce().getPublicId() : null;
        String txCode = e.getCreditTransaction() != null ? e.getCreditTransaction().getCode() : null;
        UUID txPublicId = e.getCreditTransaction() != null ? e.getCreditTransaction().getPublicId() : null;
        return new CreditLedgerEntryDTO(
                e.getPublicId(),
                e.getMovementType().name(),
                e.getAmountDelta(),
                e.getBalanceAfter(),
                annoncePublicId,
                e.getReferenceCode(),
                txCode,
                txPublicId,
                e.getCreatedAt());
    }

    /**
     * Confirme un achat de crédits (après paiement réussi Stripe/webhook ou manuel).
     * Ajoute les crédits au solde utilisateur et marque la transaction COMPLETED.
     * {@code authenticatedUserId} doit correspondre au propriétaire de la transaction (vérif en même transaction JPA que le chargement lazy de {@code user}).
     */
    public CreditTransaction confirmCreditPurchase(UUID transactionPublicId, Long authenticatedUserId) {
        CreditTransaction tx = creditTransactionRepository.findByPublicId(transactionPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit transaction not found"));
        if (!tx.getUser().getId().equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (tx.getStatus() == CreditTransaction.Status.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already completed");
        }
        User user = userRepository.findByIdForUpdate(tx.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        BigDecimal current = user.getCreditBalance() != null ? user.getCreditBalance() : BigDecimal.ZERO;
        current = current.setScale(2, RoundingMode.HALF_UP);
        BigDecimal added = tx.getCreditsAdded().setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = current.add(added).setScale(2, RoundingMode.HALF_UP);
        user.setCreditBalance(newBalance);
        userRepository.save(user);
        tx.setStatus(CreditTransaction.Status.COMPLETED);
        tx.setPaidAt(LocalDateTime.now());
        CreditTransaction savedTx = creditTransactionRepository.save(tx);
        appendLedgerCreditPurchase(user, savedTx, added, newBalance);
        return savedTx;
    }

    private void appendLedgerCreditPurchase(User user, CreditTransaction tx, BigDecimal creditsAdded, BigDecimal balanceAfter) {
        CreditLedgerEntry le = new CreditLedgerEntry();
        le.setUser(user);
        le.setMovementType(CreditLedgerMovementType.CREDIT_PURCHASE);
        le.setAmountDelta(creditsAdded);
        le.setBalanceAfter(balanceAfter);
        le.setCreditTransaction(tx);
        le.setAnnonce(null);
        le.setReferenceCode(null);
        creditLedgerEntryRepository.save(le);
    }

    /** Confirmation par paymentProviderId (ex: Stripe PaymentIntent id) pour webhook */
    public Optional<CreditTransaction> findByPaymentProviderId(String paymentProviderId) {
        return creditTransactionRepository.findByPaymentProviderId(paymentProviderId);
    }

    public Optional<CreditTransaction> findTransactionByPublicId(UUID publicId) {
        return creditTransactionRepository.findByPublicId(publicId);
    }

    /**
     * Débite le solde pour une publication d'annonce et enregistre une ligne de grand livre.
     *
     * @return id de la ligne {@code credit_ledger_entries}, ou {@code 0} si le montant est nul (aucun mouvement).
     */
    public long deductCreditsForPublication(User user, BigDecimal credits, String annonceReferenceCode) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur invalide pour le débit crédits");
        }
        if (credits == null || credits.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant de crédits invalide");
        }
        if (credits.compareTo(BigDecimal.ZERO) == 0) {
            return 0L;
        }
        if (annonceReferenceCode == null || annonceReferenceCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Référence annonce requise pour le mouvement comptable");
        }
        BigDecimal amount = credits.setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxDebit = new BigDecimal("500000");
        if (amount.compareTo(maxDebit) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coût publication incohérent (trop élevé). Contactez le support.");
        }

        User u = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        BigDecimal current = u.getCreditBalance() != null ? u.getCreditBalance() : BigDecimal.ZERO;
        current = current.setScale(2, RoundingMode.HALF_UP);
        if (current.compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Solde de crédits insuffisant. Solde: "
                            + current.stripTrailingZeros().toPlainString()
                            + ", requis: "
                            + amount.stripTrailingZeros().toPlainString());
        }
        BigDecimal newBalance = current.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        u.setCreditBalance(newBalance);
        userRepository.save(u);

        CreditLedgerEntry le = new CreditLedgerEntry();
        le.setUser(u);
        le.setMovementType(CreditLedgerMovementType.DEBIT_PUBLICATION);
        le.setAmountDelta(amount.negate());
        le.setBalanceAfter(newBalance);
        le.setCreditTransaction(null);
        le.setAnnonce(null);
        le.setReferenceCode(annonceReferenceCode.trim());
        return creditLedgerEntryRepository.save(le).getId();
    }

    /** Rattache l'annonce persistée à la ligne de livre (même transaction que la création d'annonce). */
    public void attachPublicationLedgerToAnnonce(long ledgerEntryId, long annonceId) {
        if (ledgerEntryId <= 0L) {
            return;
        }
        creditLedgerEntryRepository.attachAnnonceId(ledgerEntryId, annonceId);
    }
}
