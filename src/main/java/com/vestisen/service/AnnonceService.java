package com.vestisen.service;

import com.vestisen.dto.AnnonceCreateRequest;
import com.vestisen.dto.AnnonceDTO;
import com.vestisen.dto.AnnonceFilterRequest;
import com.vestisen.model.Annonce;
import com.vestisen.model.Category;
import com.vestisen.model.PublicationTarif;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.CartItemRepository;
import com.vestisen.repository.CategoryRepository;
import com.vestisen.repository.PublicationTarifRepository;
import com.vestisen.service.CreditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    
    public AnnonceDTO createAnnonce(AnnonceCreateRequest request, User seller) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found: " + request.getCategoryId()));
        if (!category.isActive()) {
            throw new RuntimeException("Category is not active");
        }
        
        Annonce annonce = new Annonce();
        annonce.setCode(generateUniqueCode());
        annonce.setTitle(request.getTitle());
        annonce.setDescription(request.getDescription());
        annonce.setPrice(request.getPrice());
        annonce.setCategory(category);
        annonce.setPublicationType(request.getPublicationType());
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
        
        PublicationTarif tarif = tarifRepository.findByTypeNameAndActiveTrue(request.getPublicationType())
                .orElseThrow(() -> new RuntimeException("Tarif not found for publication type: " + request.getPublicationType()));
        
        java.math.BigDecimal creditCost = tarif.getPrice();
        if (creditCost == null || creditCost.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            creditCost = java.math.BigDecimal.ZERO;
        }
        creditService.deductCredits(seller, creditCost);
        annonce.setPublicationCreditCost(creditCost);
        // expiresAt sera défini à l’approbation (décompte à partir de la publication acceptée)

        Annonce saved = annonceRepository.save(annonce);
        return toDTO(saved);
    }
    
    /** Repasse en publication Standard (durée illimitée) les annonces dont la durée de pub est dépassée. */
    @Scheduled(cron = "0 0 * * * ?")
    public void revertExpiredPublicationsToStandard() {
        List<Annonce> expired = annonceRepository.findByStatusAndExpiresAtNotNullAndExpiresAtBefore(
                Annonce.Status.APPROVED, LocalDateTime.now());
        if (expired.isEmpty()) return;
        PublicationTarif standardTarif = tarifRepository.findByTypeNameAndActiveTrue("Standard").orElse(null);
        BigDecimal standardCost = standardTarif != null && standardTarif.getPrice() != null
                ? standardTarif.getPrice() : BigDecimal.ZERO;
        for (Annonce a : expired) {
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
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);
        
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
    
    public AnnonceDTO getAnnonceById(Long id) {
        Annonce annonce = annonceRepository.findById(id)
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
        List<Annonce> annonces = annonceRepository.findByPublicationTypeAndStatusOrderByCreatedAtDesc(
            typeName, Annonce.Status.APPROVED);
        // Limiter les résultats
        return annonces.stream()
                .limit(limit)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<AnnonceDTO> getTopViewedAnnonces(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Annonce> annonces = annonceRepository.findTopViewedAnnonces(pageable);
        return annonces.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    public void incrementContactCount(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        if (annonce.getStatus() != Annonce.Status.APPROVED) {
            return;
        }
        annonce.setContactCount(annonce.getContactCount() + 1);
        annonceRepository.save(annonce);
    }
    
    public AnnonceDTO approveAnnonce(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        annonce.setStatus(Annonce.Status.APPROVED);
        annonceRepository.save(annonce);
        applicationEventPublisher.publishEvent(new com.vestisen.event.AnnonceApprovedEvent(this, annonce));
        annonce = annonceRepository.findById(id).orElseThrow(() -> new RuntimeException("Annonce not found"));
        return toDTO(annonce);
    }
    
    public AnnonceDTO rejectAnnonce(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        annonce.setStatus(Annonce.Status.REJECTED);
        return toDTO(annonceRepository.save(annonce));
    }
    
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

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
        dto.setId(annonce.getId());
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
        dto.setSellerId(annonce.getSeller().getId());
        dto.setSellerName(annonce.getSeller().getFirstName() + " " + annonce.getSeller().getLastName());
        dto.setSellerPhone(annonce.getSeller().getPhone());
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
        List<Annonce> list = annonceRepository.findByBuyer_IdOrderByCreatedAtDesc(buyerId, PageRequest.of(0, 100));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Pour le panier / listes internes : retourne le DTO sans vérifier le statut ni incrémenter les vues. */
    public AnnonceDTO getAnnonceDTOById(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        return toDTO(annonce);
    }

    /**
     * Confirmer l'achat d'une annonce par un client : définit l'acheteur, passe le statut à SOLD,
     * et retire l'annonce du panier de l'acheteur.
     */
    public AnnonceDTO buyAnnonce(Long annonceId, User buyer) {
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
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
        cartItemRepository.findByUserIdAndAnnonceId(buyer.getId(), annonceId).ifPresent(cartItemRepository::delete);
        return toDTO(annonce);
    }

    /**
     * Upload des photos pour une annonce. Stockage dans annonce/user/{codeAnnonce}/
     * Seul le vendeur de l'annonce peut ajouter des photos.
     */
    public AnnonceDTO addPhotos(Long annonceId, User currentUser, MultipartFile[] files) throws IOException {
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        if (!annonce.getSeller().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres annonces");
        }
        String code = annonce.getCode();
        if (code == null || code.isBlank()) {
            throw new RuntimeException("Annonce sans code");
        }
        List<String> newPaths = fileStorageService.storeAnnoncePhotos(code, files);
        List<String> images = annonce.getImages() != null ? new ArrayList<>(annonce.getImages()) : new ArrayList<>();
        images.addAll(newPaths);
        annonce.setImages(images);
        annonceRepository.save(annonce);
        return toDTO(annonce);
    }
}
