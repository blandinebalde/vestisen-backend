package com.vestisen.service;

import com.vestisen.dto.AnnonceCreateRequest;
import com.vestisen.dto.AnnonceDTO;
import com.vestisen.dto.AnnonceFilterRequest;
import com.vestisen.model.Annonce;
import com.vestisen.model.PublicationTarif;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.PublicationTarifRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnonceService {
    
    @Autowired
    private AnnonceRepository annonceRepository;
    
    @Autowired
    private PublicationTarifRepository tarifRepository;
    
    public AnnonceDTO createAnnonce(AnnonceCreateRequest request, User seller) {
        Annonce annonce = new Annonce();
        annonce.setTitle(request.getTitle());
        annonce.setDescription(request.getDescription());
        annonce.setPrice(request.getPrice());
        annonce.setCategory(request.getCategory());
        annonce.setPublicationType(request.getPublicationType());
        annonce.setCondition(request.getCondition());
        annonce.setSize(request.getSize());
        annonce.setBrand(request.getBrand());
        annonce.setColor(request.getColor());
        annonce.setLocation(request.getLocation());
        annonce.setImages(request.getImages() != null ? request.getImages() : List.of());
        annonce.setSeller(seller);
        annonce.setStatus(Annonce.Status.PENDING);
        
        // Calculer la date d'expiration basée sur le tarif
        PublicationTarif tarif = tarifRepository.findByPublicationTypeAndActiveTrue(request.getPublicationType())
                .orElseThrow(() -> new RuntimeException("Tarif not found for publication type"));
        
        annonce.setExpiresAt(LocalDateTime.now().plusDays(tarif.getDurationDays()));
        
        Annonce saved = annonceRepository.save(annonce);
        return toDTO(saved);
    }
    
    public Page<AnnonceDTO> searchAnnonces(AnnonceFilterRequest filter) {
        // Logique de visibilité selon le type de publication :
        // - Standard : visibilité normale
        // - Premium : visibilité prioritaire dans la catégorie (triées en premier)
        // - Top Pub : mise en avant sur la page d'accueil (géré via getTopAnnonces)
        
        // Créer un tri personnalisé qui priorise les Premium et Top Pub
        // Ordre de priorité : TOP_PUB > PREMIUM > STANDARD
        Sort sort;
        
        if ("publicationType".equals(filter.getSortBy())) {
            // Si le tri est déjà sur publicationType, utiliser le sens demandé
            sort = Sort.by(filter.getSortDir().equalsIgnoreCase("ASC") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC, "publicationType");
        } else {
            // Sinon, prioriser par type de publication puis par le critère demandé
            sort = Sort.by(
                // Priorité : TOP_PUB (2) > PREMIUM (1) > STANDARD (0)
                // On utilise un tri descendant pour avoir TOP_PUB en premier
                Sort.Order.desc("publicationType"),
                // Puis le tri demandé par l'utilisateur
                filter.getSortDir().equalsIgnoreCase("ASC") 
                    ? Sort.Order.asc(filter.getSortBy())
                    : Sort.Order.desc(filter.getSortBy())
            );
        }
        
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);
        
        Page<Annonce> annonces = annonceRepository.searchAnnonces(
            Annonce.Status.APPROVED,
            filter.getCategory(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            filter.getSize(),
            filter.getBrand(),
            filter.getCondition(),
            filter.getSearch(),
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
        
        // Incrémenter le compteur de vues
        annonce.setViewCount(annonce.getViewCount() + 1);
        annonceRepository.save(annonce);
        
        return toDTO(annonce);
    }
    
    public List<AnnonceDTO> getTopAnnonces(Annonce.PublicationType type, int limit) {
        List<Annonce> annonces = annonceRepository.findByPublicationTypeAndStatusOrderByCreatedAtDesc(
            type, Annonce.Status.APPROVED);
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
        annonce.setContactCount(annonce.getContactCount() + 1);
        annonceRepository.save(annonce);
    }
    
    public AnnonceDTO approveAnnonce(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        annonce.setStatus(Annonce.Status.APPROVED);
        annonce.setPublishedAt(LocalDateTime.now());
        return toDTO(annonceRepository.save(annonce));
    }
    
    public AnnonceDTO rejectAnnonce(Long id) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        annonce.setStatus(Annonce.Status.REJECTED);
        return toDTO(annonceRepository.save(annonce));
    }
    
    public AnnonceDTO toDTO(Annonce annonce) {
        AnnonceDTO dto = new AnnonceDTO();
        dto.setId(annonce.getId());
        dto.setTitle(annonce.getTitle());
        dto.setDescription(annonce.getDescription());
        dto.setPrice(annonce.getPrice());
        dto.setCategory(annonce.getCategory());
        dto.setPublicationType(annonce.getPublicationType());
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
        return dto;
    }
}
