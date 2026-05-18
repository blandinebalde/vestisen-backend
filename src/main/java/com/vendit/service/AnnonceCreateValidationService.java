package com.vendit.service;

import com.vendit.dto.AnnonceCreateRequest;
import com.vendit.dto.AnnonceValidationResponseDTO;
import com.vendit.model.Category;
import com.vendit.model.SellerSubscription;
import com.vendit.model.PublicationPaymentMethod;
import com.vendit.model.PublicationTarif;
import com.vendit.model.User;
import com.vendit.repository.CategoryRepository;
import com.vendit.repository.PublicationTarifRepository;
import com.vendit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnnonceCreateValidationService {

    public static final int MAX_TITLE = 200;
    public static final int MAX_DESCRIPTION = 2000;
    public static final int MAX_PHOTOS = 5;
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PublicationTarifRepository tarifRepository;

    @Autowired
    private SellerPlanService sellerPlanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellerSubscriptionService sellerSubscriptionService;

    public AnnonceValidationResponseDTO validateDetails(AnnonceCreateRequest request, User seller) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateCoreFields(request, errors, false);
        validateOptionalFields(request, errors);

        if (errors.isEmpty()) {
            return AnnonceValidationResponseDTO.ok("details");
        }
        return AnnonceValidationResponseDTO.fail("details", errors);
    }

    public AnnonceValidationResponseDTO validateVisibility(AnnonceCreateRequest request, User seller) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null) {
            errors.put("_form", "Données du formulaire manquantes.");
            return AnnonceValidationResponseDTO.fail("visibility", errors);
        }

        String publicationType = request.getPublicationType() != null ? request.getPublicationType().trim() : "";
        if (publicationType.isEmpty()) {
            errors.put("publicationType", "Veuillez choisir un type de publication.");
        } else if (publicationType.length() > 100) {
            errors.put("publicationType", "Type de publication invalide.");
        } else if (tarifRepository.findByTypeNameAndActiveTrue(publicationType).isEmpty()) {
            errors.put("publicationType", "Type de publication inconnu ou inactif.");
        }

        PublicationTarif tarif = resolveTarif(request, errors);
        PublicationPaymentMethod paymentMethod = parsePaymentMethod(request, errors);
        validatePaymentEligibility(seller, tarif, paymentMethod, errors);

        AnnonceValidationResponseDTO response = errors.isEmpty()
                ? AnnonceValidationResponseDTO.ok("visibility")
                : AnnonceValidationResponseDTO.fail("visibility", errors);
        if (errors.isEmpty()) {
            enrichPublicationWarnings(request, seller, response);
        }
        return response;
    }

    public AnnonceValidationResponseDTO validatePhotos(MultipartFile[] files, User seller) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (files == null || files.length == 0) {
            errors.put("photos", "Ajoutez au moins une photo.");
            return AnnonceValidationResponseDTO.fail("photos", errors);
        }
        if (files.length > MAX_PHOTOS) {
            errors.put("photos", "Maximum " + MAX_PHOTOS + " photos autorisées.");
        }
        int validCount = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                errors.put("photos", "Un ou plusieurs fichiers sont vides.");
                break;
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                errors.put("photos", "Format non accepté pour « " + safeName(file) + " » (JPG, PNG, WebP, GIF uniquement).");
                break;
            }
            if (file.getSize() > MAX_FILE_BYTES) {
                errors.put("photos", "Fichier trop volumineux : « " + safeName(file) + " » (max 5 Mo).");
                break;
            }
            validCount++;
        }
        if (!errors.isEmpty()) {
            return AnnonceValidationResponseDTO.fail("photos", errors);
        }
        AnnonceValidationResponseDTO ok = AnnonceValidationResponseDTO.ok("photos");
        ok.getWarnings().put("photoCount", String.valueOf(validCount));
        return ok;
    }

    public AnnonceValidationResponseDTO validateConfirm(AnnonceCreateRequest request, User seller) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateCoreFields(request, errors, true);
        validateOptionalFields(request, errors);
        PublicationTarif tarif = resolveTarif(request, errors);
        PublicationPaymentMethod paymentMethod = parsePaymentMethod(request, errors);
        validatePaymentEligibility(seller, tarif, paymentMethod, errors);

        if (!errors.isEmpty()) {
            return AnnonceValidationResponseDTO.fail("confirm", errors);
        }
        return AnnonceValidationResponseDTO.ok("confirm");
    }

    private void validateCoreFields(AnnonceCreateRequest request, Map<String, String> errors, boolean requirePublication) {
        if (request == null) {
            errors.put("_form", "Données du formulaire manquantes.");
            return;
        }

        String title = request.getTitle() != null ? request.getTitle().trim() : "";
        if (title.isEmpty()) {
            errors.put("title", "Le titre est obligatoire.");
        } else if (title.length() > MAX_TITLE) {
            errors.put("title", "Le titre ne doit pas dépasser " + MAX_TITLE + " caractères.");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : "";
        if (description.length() > MAX_DESCRIPTION) {
            errors.put("description", "La description ne doit pas dépasser " + MAX_DESCRIPTION + " caractères.");
        }

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ONE) < 0) {
            errors.put("price", "Le prix doit être supérieur ou égal à 1 FCFA.");
        }

        if (request.getCategoryId() == null) {
            errors.put("categoryId", "Veuillez choisir une catégorie.");
        } else {
            Category category = categoryRepository.findById(request.getCategoryId()).orElse(null);
            if (category == null) {
                errors.put("categoryId", "Catégorie introuvable.");
            } else if (!category.isActive()) {
                errors.put("categoryId", "Cette catégorie n'est plus disponible.");
            }
        }

        if (requirePublication) {
            String publicationType = request.getPublicationType() != null ? request.getPublicationType().trim() : "";
            if (publicationType.isEmpty()) {
                errors.put("publicationType", "Veuillez choisir un type de publication.");
            } else if (publicationType.length() > 100) {
                errors.put("publicationType", "Type de publication invalide.");
            } else if (tarifRepository.findByTypeNameAndActiveTrue(publicationType).isEmpty()) {
                errors.put("publicationType", "Type de publication inconnu ou inactif.");
            }
        }
    }

    private void validateOptionalFields(AnnonceCreateRequest request, Map<String, String> errors) {
        if (request.getToutDoitPartir() != null && Boolean.TRUE.equals(request.getToutDoitPartir())) {
            if (request.getOriginalPrice() != null && request.getPrice() != null
                    && request.getOriginalPrice().compareTo(request.getPrice()) < 0) {
                errors.put("originalPrice", "Le prix barré doit être supérieur ou égal au prix de vente.");
            }
        }
        if (request.getLatitude() != null && (request.getLatitude() < -90 || request.getLatitude() > 90)) {
            errors.put("latitude", "Latitude invalide.");
        }
        if (request.getLongitude() != null && (request.getLongitude() < -180 || request.getLongitude() > 180)) {
            errors.put("longitude", "Longitude invalide.");
        }
    }

    private PublicationTarif resolveTarif(AnnonceCreateRequest request, Map<String, String> errors) {
        String publicationType = request.getPublicationType() != null ? request.getPublicationType().trim() : "";
        if (publicationType.isEmpty()) {
            return null;
        }
        return tarifRepository.findByTypeNameAndActiveTrue(publicationType)
                .orElseGet(() -> {
                    if (!errors.containsKey("publicationType")) {
                        errors.put("publicationType", "Type de publication inconnu ou inactif.");
                    }
                    return null;
                });
    }

    private PublicationPaymentMethod parsePaymentMethod(AnnonceCreateRequest request, Map<String, String> errors) {
        String raw = request.getPaymentMethod();
        if (raw == null || raw.isBlank()) {
            return PublicationPaymentMethod.CREDITS;
        }
        try {
            return PublicationPaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.put("paymentMethod", "Mode de paiement invalide (CREDITS ou SUBSCRIPTION).");
            return PublicationPaymentMethod.CREDITS;
        }
    }

    private void validatePaymentEligibility(
            User seller,
            PublicationTarif tarif,
            PublicationPaymentMethod paymentMethod,
            Map<String, String> errors) {
        if (tarif == null || seller == null) {
            return;
        }
        User fresh = userRepository.findById(seller.getId()).orElse(seller);

        if (paymentMethod == PublicationPaymentMethod.SUBSCRIPTION) {
            if (!sellerPlanService.canPayWithSubscription(fresh)) {
                if (!sellerPlanService.isSubscriptionPeriodActive(fresh)) {
                    errors.put("paymentMethod", "Votre abonnement n'est plus actif. Renouvelez-le ou payez en crédits.");
                } else {
                    errors.put("paymentMethod", "Quota de publications actives atteint pour votre plan.");
                }
            } else if (tarif.isTopPublication() && !hasBoostForTop(fresh)) {
                errors.put("publicationType", "Aucun boost top publication restant sur votre plan ce mois-ci.");
            }
            return;
        }

        BigDecimal creditCost = tarif.getPrice() != null ? tarif.getPrice() : BigDecimal.ZERO;
        if (creditCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balance = fresh.getCreditBalance() != null ? fresh.getCreditBalance() : BigDecimal.ZERO;
            if (balance.compareTo(creditCost) < 0) {
                errors.put("paymentMethod",
                        "Solde de crédits insuffisant (requis : "
                                + creditCost.stripTrailingZeros().toPlainString()
                                + ", disponible : "
                                + balance.stripTrailingZeros().toPlainString()
                                + ").");
            }
        }
    }

    private boolean hasBoostForTop(User seller) {
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(seller);
        return sub.getBoostsRemaining() > 0;
    }

    private void enrichPublicationWarnings(AnnonceCreateRequest request, User seller, AnnonceValidationResponseDTO response) {
        String publicationType = request.getPublicationType() != null ? request.getPublicationType().trim() : "";
        if (publicationType.isEmpty()) {
            return;
        }
        tarifRepository.findByTypeNameAndActiveTrue(publicationType).ifPresent(tarif -> {
            PublicationPaymentMethod pm = parsePaymentMethod(request, new LinkedHashMap<>());
            if (pm == PublicationPaymentMethod.SUBSCRIPTION && tarif.isTopPublication() && !hasBoostForTop(seller)) {
                response.getWarnings().put("boosts",
                        "Ce type est une top publication : aucun boost restant si vous payez via abonnement.");
            }
        });
    }

    private static String safeName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && !name.isBlank() ? name : "fichier";
    }
}
