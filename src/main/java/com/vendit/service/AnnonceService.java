package com.vendit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.vendit.dto.AnnonceCreateRequest;
import com.vendit.dto.AnnonceDTO;
import com.vendit.dto.AnnonceFilterRequest;
import com.vendit.dto.AnnonceSellerUpdateRequest;
import com.vendit.dto.MyAnnoncesSummaryDTO;
import com.vendit.config.CatalogPageLimits;
import com.vendit.model.Annonce;
import com.vendit.model.Category;
import com.vendit.model.PublicationPaymentMethod;
import com.vendit.model.PublicationTarif;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.CartItemRepository;
import com.vendit.repository.CategoryRepository;
import com.vendit.repository.PublicationTarifRepository;
import com.vendit.service.CreditService;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Math.*;

@Service
@Transactional
public class AnnonceService {
    
    @Autowired
    private AnnonceRepository annonceRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private PublicationTarifRepository tarifRepository;
    
    @Autowired
    private CreditService creditService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ActionLogService actionLogService;

    @Autowired
    private SellerPlanService sellerPlanService;
    
    public AnnonceDTO createAnnonce(AnnonceCreateRequest request, User seller) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));
        if (!category.isActive()) {
            throw new RuntimeException("Category is not active");
        }

        String publicationType = request.getPublicationType() == null ? "" : request.getPublicationType().trim();
        if (publicationType.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de publication requis");
        }
        if (publicationType.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de publication invalide");
        }

        Annonce annonce = new Annonce();
        annonce.setCode(generateUniqueCode());
        annonce.setTitle(request.getTitle());
        annonce.setDescription(request.getDescription());
        annonce.setPrice(request.getPrice());
        annonce.setCategory(category);
        annonce.setPublicationType(publicationType);
        annonce.setCondition(request.getCondition());
        annonce.setSize(request.getSize());
        annonce.setBrand(request.getBrand());
        annonce.setColor(request.getColor());
        annonce.setLocation(request.getLocation());
        annonce.setImages(request.getImages() != null ? request.getImages() : List.of());
        annonce.setSeller(seller);
        annonce.setStatus(Annonce.Status.PENDING);
        
        if (request.getToutDoitPartir() != null) annonce.setToutDoitPartir(request.getToutDoitPartir());
        if (request.getOriginalPrice() != null) annonce.setOriginalPrice(request.getOriginalPrice());
        if (request.getIsLot() != null) annonce.setLot(request.getIsLot());
        if (request.getAcceptPaymentOnDelivery() != null) annonce.setAcceptPaymentOnDelivery(request.getAcceptPaymentOnDelivery());
        if (request.getLatitude() != null) annonce.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) annonce.setLongitude(request.getLongitude());
        
        PublicationTarif tarif = tarifRepository.findByTypeNameAndActiveTrue(publicationType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Type de publication inconnu ou inactif : " + publicationType));

        PublicationPaymentMethod paymentMethod = parsePaymentMethod(request.getPaymentMethod());

        if (paymentMethod == PublicationPaymentMethod.SUBSCRIPTION) {
            sellerPlanService.applySubscriptionPublication(seller, tarif);
            annonce.setPublicationCreditCost(BigDecimal.ZERO);
            annonce.setPublicationPaymentMethod(PublicationPaymentMethod.SUBSCRIPTION);
            return toDTO(annonceRepository.save(annonce));
        }

        BigDecimal creditCost = tarif.getPrice();
        if (creditCost == null || creditCost.compareTo(BigDecimal.ZERO) <= 0) {
            creditCost = BigDecimal.ZERO;
        }
        long ledgerEntryId = creditService.deductCreditsForPublication(seller, creditCost, annonce.getCode());
        annonce.setPublicationCreditCost(creditCost);
        annonce.setPublicationPaymentMethod(PublicationPaymentMethod.CREDITS);

        Annonce saved = annonceRepository.save(annonce);
        creditService.attachPublicationLedgerToAnnonce(ledgerEntryId, saved.getId());
        return toDTO(saved);
    }
    
    /** Repasse en publication Standard (durée illimitée) les annonces dont la durée de pub est dépassée. */
    @Scheduled(cron = "0 0 * * * ?")
    public void revertExpiredPublicationsToStandard() {
        LocalDateTime now = LocalDateTime.now();
        PublicationTarif standardTarif = tarifRepository.findByTypeNameAndActiveTrue("Standard").orElse(null);
        BigDecimal standardCost = standardTarif != null && standardTarif.getPrice() != null
                ? standardTarif.getPrice() : BigDecimal.ZERO;

        while (true) {
            Page<Annonce> batch = annonceRepository.findByStatusAndExpiresAtNotNullAndExpiresAtBefore(
                    Annonce.Status.APPROVED,
                    now,
                    PageRequest.of(0, CatalogPageLimits.EXPIRED_ANNONCE_BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (Annonce a : batch.getContent()) {
                a.setPublicationType("Standard");
                a.setPublicationCreditCost(standardCost);
                a.setExpiresAt(null);
                annonceRepository.save(a);
                User seller = a.getSeller();
                actionLogService.logInternalAction(
                        seller != null ? seller.getId() : null,
                        seller != null ? seller.getEmail() : "system",
                        seller != null && seller.getRole() != null ? seller.getRole().name() : null,
                        "Annonce repassée en Standard (durée dépassée)",
                        "annonce",
                        a.getId(),
                        true);
            }
        }
    }

    /** Si l'annonce a une durée dépassée, la repasse en Standard durée illimitée et sauvegarde. */
    public Annonce revertToStandardIfExpired(Annonce annonce) {
        if (annonce.getExpiresAt() == null || !annonce.getExpiresAt().isBefore(LocalDateTime.now()))
            return annonce;
        PublicationTarif standardTarif = tarifRepository.findByTypeNameAndActiveTrue("Standard").orElse(null);
        BigDecimal standardCost = standardTarif != null && standardTarif.getPrice() != null
                ? standardTarif.getPrice() : BigDecimal.ZERO;
        annonce.setPublicationType("Standard");
        annonce.setPublicationCreditCost(standardCost);
        annonce.setExpiresAt(null);
        Annonce saved = annonceRepository.save(annonce);
        User seller = saved.getSeller();
        actionLogService.logInternalAction(
                seller != null ? seller.getId() : null,
                seller != null ? seller.getEmail() : "system",
                seller != null && seller.getRole() != null ? seller.getRole().name() : null,
                "Annonce repassée en Standard (durée dépassée)",
                "annonce",
                saved.getId(),
                true);
        return saved;
    }

    public Page<AnnonceDTO> searchAnnonces(AnnonceFilterRequest filter) {
        revertExpiredPublicationsToStandard();
        // Tri catalogue : annonces avec le plus de crédits (type de pub) en premier, puis par date décroissante
        Sort sort = Sort.by(
                Sort.Order.desc("publicationCreditCost").with(Sort.NullHandling.NULLS_LAST),
                Sort.Order.desc("createdAt")
        );
        int page = CatalogPageLimits.clampPageIndex(filter.getPage());
        int size = CatalogPageLimits.clampPageSize(filter.getPageSize());
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Double latMin = null, latMax = null, lngMin = null, lngMax = null;
        if (filter.getLatitude() != null && filter.getLongitude() != null && filter.getRadiusKm() != null && filter.getRadiusKm() > 0) {
            double deltaLat = filter.getRadiusKm() / 111.0;
            double deltaLng = filter.getRadiusKm() / (111.0 * max(0.01, cos(toRadians(filter.getLatitude()))));
            latMin = filter.getLatitude() - deltaLat;
            latMax = filter.getLatitude() + deltaLat;
            lngMin = filter.getLongitude() - deltaLng;
            lngMax = filter.getLongitude() + deltaLng;
        }
        
        Page<Annonce> annonces = annonceRepository.searchAnnonces(
            Annonce.Status.APPROVED,
            filter.getCategoryId(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            filter.getSize(),
            filter.getBrand(),
            filter.getCondition(),
            filter.getSearch(),
            filter.getToutDoitPartir(),
            latMin, latMax, lngMin, lngMax,
            pageable
        );
        
        return annonces.map(this::toDTO);
    }
    
    public AnnonceDTO getAnnonceByPublicId(UUID publicId) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        
        // Ne retourner que les annonces approuvées pour les utilisateurs publics
        if (annonce.getStatus() != Annonce.Status.APPROVED) {
            throw new RuntimeException("Annonce not available");
        }
        annonce = revertToStandardIfExpired(annonce);
        annonce.setViewCount(annonce.getViewCount() + 1);
        annonceRepository.save(annonce);
        return toDTO(annonce);
    }
    
    public List<AnnonceDTO> getTopAnnonces(String typeName, int limit) {
        revertExpiredPublicationsToStandard();
        int safe = CatalogPageLimits.clampTopLimit(limit);
        Pageable pageable = PageRequest.of(0, safe);
        List<Annonce> annonces = annonceRepository.findByPublicationTypeAndStatusOrderByCreatedAtDesc(
                typeName, Annonce.Status.APPROVED, pageable);
        return annonces.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    public List<AnnonceDTO> getTopViewedAnnonces(int limit) {
        Pageable pageable = PageRequest.of(0, CatalogPageLimits.clampTopLimit(limit));
        List<Annonce> annonces = annonceRepository.findTopViewedAnnonces(pageable);
        return annonces.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    public void incrementContactCount(UUID publicId) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        if (annonce.getStatus() != Annonce.Status.APPROVED) {
            return;
        }
        annonce.setContactCount(annonce.getContactCount() + 1);
        annonceRepository.save(annonce);
    }
    
    public AnnonceDTO approveAnnonce(UUID publicId) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        annonce.setStatus(Annonce.Status.APPROVED);
        annonceRepository.save(annonce);
        applicationEventPublisher.publishEvent(new com.vendit.event.AnnonceApprovedEvent(this, annonce));
        annonce = annonceRepository.findByPublicId(publicId).orElseThrow(() -> new RuntimeException("Annonce not found"));
        return toDTO(annonce);
    }
    
    public AnnonceDTO rejectAnnonce(UUID publicId) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        annonce.setStatus(Annonce.Status.REJECTED);
        return toDTO(annonceRepository.save(annonce));
    }
    
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static PublicationPaymentMethod parsePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return PublicationPaymentMethod.CREDITS;
        }
        try {
            return PublicationPaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode de paiement invalide (CREDITS ou SUBSCRIPTION)");
        }
    }

    private String generateUniqueCode() {
        StringBuilder sb = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        String code = sb.toString();
        if (annonceRepository.existsByCode(code)) {
            return generateUniqueCode();
        }
        return code;
    }

    public AnnonceDTO toDTO(Annonce annonce) {
        AnnonceDTO dto = new AnnonceDTO();
        dto.setPublicId(annonce.getPublicId());
        dto.setCode(annonce.getCode());
        dto.setTitle(annonce.getTitle());
        dto.setDescription(annonce.getDescription());
        dto.setPrice(annonce.getPrice());
        if (annonce.getCategory() != null) {
            dto.setCategoryId(annonce.getCategory().getId());
            dto.setCategoryName(annonce.getCategory().getName());
        }
        dto.setPublicationType(annonce.getPublicationType());
        dto.setPublicationCreditCost(annonce.getPublicationCreditCost());
        dto.setCondition(annonce.getCondition());
        dto.setSize(annonce.getSize());
        dto.setBrand(annonce.getBrand());
        dto.setColor(annonce.getColor());
        dto.setLocation(annonce.getLocation());
        dto.setImages(annonce.getImages());
        dto.setSellerPublicId(annonce.getSeller().getPublicId());
        dto.setSellerName(annonce.getSeller().getFirstName() + " " + annonce.getSeller().getLastName());
        dto.setSellerPhone(annonce.getSeller().getPhone());
        dto.setSellerWhatsapp(annonce.getSeller().getWhatsapp());
        dto.setStatus(annonce.getStatus());
        dto.setViewCount(annonce.getViewCount());
        dto.setContactCount(annonce.getContactCount());
        dto.setCreatedAt(annonce.getCreatedAt());
        dto.setPublishedAt(annonce.getPublishedAt());
        dto.setExpiresAt(annonce.getExpiresAt());
        dto.setToutDoitPartir(annonce.isToutDoitPartir());
        dto.setOriginalPrice(annonce.getOriginalPrice());
        dto.setLot(annonce.isLot());
        dto.setAcceptPaymentOnDelivery(annonce.isAcceptPaymentOnDelivery());
        dto.setLatitude(annonce.getLatitude());
        dto.setLongitude(annonce.getLongitude());
        return dto;
    }

    public List<AnnonceDTO> getMyPurchases(Long buyerId) {
        List<Annonce> list = annonceRepository.findByBuyer_IdOrderByCreatedAtDesc(buyerId,
                PageRequest.of(0, CatalogPageLimits.MY_PURCHASES_MAX));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Pour le panier / listes internes : retourne le DTO sans vérifier le statut ni incrémenter les vues. */
    public AnnonceDTO getAnnonceDTOByPublicId(UUID publicId) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        return toDTO(annonce);
    }

    /**
     * Confirmer l'achat d'une annonce par un client : définit l'acheteur, passe le statut à SOLD,
     * et retire l'annonce du panier de l'acheteur.
     */
    public AnnonceDTO buyAnnonce(UUID annoncePublicId, User buyer) {
        Annonce annonce = annonceRepository.findByPublicIdForUpdate(annoncePublicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        Long annonceInternalId = annonce.getId();
        if (annonce.getSeller().getId().equals(buyer.getId())) {
            throw new RuntimeException("Vous ne pouvez pas acheter votre propre annonce");
        }
        if (annonce.getStatus() == Annonce.Status.SOLD) {
            throw new RuntimeException("Cette annonce est déjà vendue");
        }
        if (annonce.getStatus() != Annonce.Status.APPROVED && annonce.getStatus() != Annonce.Status.PENDING) {
            throw new RuntimeException("Annonce non disponible à la vente");
        }
        annonce.setBuyer(buyer);
        annonce.setStatus(Annonce.Status.SOLD);
        annonceRepository.save(annonce);
        sellerPlanService.recordSaleCommission(annonce, buyer);
        cartItemRepository.findByUserIdAndAnnonceId(buyer.getId(), annonceInternalId).ifPresent(cartItemRepository::delete);
        return toDTO(annonce);
    }

    /** Liste paginée des annonces du vendeur, filtre optionnel par statut et recherche titre / description / code. */
    public Page<AnnonceDTO> listMyAnnonces(User seller, int page, int size, String statusParam, String searchRaw) {
        Annonce.Status statusFilter = parseSellerListStatus(statusParam);
        String search = normalizeSellerSearch(searchRaw);
        Pageable pageable = PageRequest.of(
                CatalogPageLimits.clampPageIndex(page),
                CatalogPageLimits.clampPageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Annonce> pageResult = annonceRepository.findBySellerIdFiltered(
                seller.getId(), statusFilter, search, pageable);
        return pageResult.map(this::toDTO);
    }

    /** Comptages et totaux vues / contacts pour le tableau de bord vendeur. */
    public MyAnnoncesSummaryDTO getMyAnnoncesSummary(User seller) {
        Long sid = seller.getId();
        MyAnnoncesSummaryDTO dto = new MyAnnoncesSummaryDTO();
        Object[] sums = annonceRepository.sumViewsAndContactsForSeller(sid);
        if (sums != null && sums.length >= 2) {
            dto.setTotalViews(sums[0] == null ? 0L : ((Number) sums[0]).longValue());
            dto.setTotalContacts(sums[1] == null ? 0L : ((Number) sums[1]).longValue());
        }
        List<Object[]> rows = annonceRepository.countBySellerIdGroupByStatus(sid);
        long total = 0L;
        if (rows != null) {
            for (Object[] row : rows) {
                if (row == null || row.length < 2) {
                    continue;
                }
                Annonce.Status st = (Annonce.Status) row[0];
                long c = ((Number) row[1]).longValue();
                total += c;
                switch (st) {
                    case PENDING -> dto.setPendingCount(c);
                    case APPROVED -> dto.setApprovedCount(c);
                    case REJECTED -> dto.setRejectedCount(c);
                    case SOLD -> dto.setSoldCount(c);
                    case EXPIRED -> dto.setExpiredCount(c);
                    default -> {
                    }
                }
            }
        }
        dto.setTotalCount(total);
        return dto;
    }

    private static Annonce.Status parseSellerListStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return null;
        }
        String s = statusParam.trim();
        if ("ALL".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return Annonce.Status.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut inconnu : " + statusParam);
        }
    }

    private static String normalizeSellerSearch(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    /** Détail vendeur : toute annonce dont l’utilisateur est propriétaire (tous statuts). */
    public AnnonceDTO getMyAnnonceForSeller(UUID publicId, User seller) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce introuvable"));
        if (!annonce.getSeller().getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette annonce ne vous appartient pas");
        }
        return toDTO(annonce);
    }

    /** Mise à jour par le vendeur (statut, type de publication et coût crédits inchangés). */
    public AnnonceDTO updateMyAnnonceForSeller(UUID publicId, User seller, AnnonceSellerUpdateRequest req) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce introuvable"));
        if (!annonce.getSeller().getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette annonce ne vous appartient pas");
        }
        if (annonce.getStatus() == Annonce.Status.SOLD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une annonce vendue ne peut plus être modifiée");
        }

        if (req.getTitle() != null) {
            String t = req.getTitle().trim();
            if (t.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le titre ne peut pas être vide");
            }
            annonce.setTitle(t);
        }
        if (req.getDescription() != null) {
            annonce.setDescription(req.getDescription().trim());
        }
        if (req.getPrice() != null) {
            if (req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prix invalide");
            }
            annonce.setPrice(req.getPrice());
        }
        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie inconnue"));
            if (!category.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie inactive");
            }
            annonce.setCategory(category);
        }
        if (req.getCondition() != null) {
            annonce.setCondition(req.getCondition());
        }
        if (req.getSize() != null) {
            annonce.setSize(req.getSize().trim().isEmpty() ? null : req.getSize().trim());
        }
        if (req.getBrand() != null) {
            annonce.setBrand(req.getBrand().trim().isEmpty() ? null : req.getBrand().trim());
        }
        if (req.getColor() != null) {
            annonce.setColor(req.getColor().trim().isEmpty() ? null : req.getColor().trim());
        }
        if (req.getLocation() != null) {
            annonce.setLocation(req.getLocation().trim().isEmpty() ? null : req.getLocation().trim());
        }
        if (req.getImages() != null) {
            annonce.setImages(new ArrayList<>(req.getImages()));
        }
        if (req.getToutDoitPartir() != null) {
            annonce.setToutDoitPartir(req.getToutDoitPartir());
        }
        if (req.getOriginalPrice() != null) {
            annonce.setOriginalPrice(req.getOriginalPrice());
        }
        if (req.getIsLot() != null) {
            annonce.setLot(req.getIsLot());
        }
        if (req.getAcceptPaymentOnDelivery() != null) {
            annonce.setAcceptPaymentOnDelivery(req.getAcceptPaymentOnDelivery());
        }
        if (req.getLatitude() != null) {
            annonce.setLatitude(req.getLatitude());
        }
        if (req.getLongitude() != null) {
            annonce.setLongitude(req.getLongitude());
        }

        return toDTO(annonceRepository.save(annonce));
    }

    /**
     * Suppression par le vendeur : uniquement annonces en attente ou rejetées (évite les FK
     * conversations / paiements sur annonces déjà en ligne ou vendues).
     */
    public void deleteMyAnnonceForSeller(UUID publicId, User seller) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Annonce introuvable"));
        if (!annonce.getSeller().getId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette annonce ne vous appartient pas");
        }
        if (annonce.getStatus() != Annonce.Status.PENDING && annonce.getStatus() != Annonce.Status.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seules les annonces en attente de modération ou rejetées peuvent être supprimées");
        }
        cartItemRepository.deleteByAnnonce_Id(annonce.getId());
        annonceRepository.delete(annonce);
    }

    /**
     * Upload des photos pour une annonce. Stockage dans annonce/user/{userCode}/{annonceCode}/
     * Seul le vendeur de l'annonce peut ajouter des photos.
     */
    public AnnonceDTO addPhotos(UUID annoncePublicId, User currentUser, MultipartFile[] files) throws IOException {
        Annonce annonce = annonceRepository.findByPublicId(annoncePublicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        if (!annonce.getSeller().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres annonces");
        }
        if (annonce.getStatus() == Annonce.Status.SOLD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Impossible d’ajouter des photos à une annonce vendue");
        }
        String annonceCode = annonce.getCode();
        if (annonceCode == null || annonceCode.isBlank()) {
            throw new RuntimeException("Annonce sans code");
        }
        String userCode = annonce.getSeller().getCode();
        if (userCode == null || userCode.isBlank()) {
            throw new RuntimeException("Vendeur sans code");
        }
        List<String> newPaths = fileStorageService.storeAnnoncePhotos(userCode, annonceCode, files);
        List<String> images = annonce.getImages() != null ? new ArrayList<>(annonce.getImages()) : new ArrayList<>();
        images.addAll(newPaths);
        annonce.setImages(images);
        annonceRepository.save(annonce);
        return toDTO(annonce);
    }
}
