package com.vestisen.controller;

import com.vestisen.dto.*;
import com.vestisen.model.Annonce;
import com.vestisen.model.Category;
import com.vestisen.model.PublicationTarif;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.CategoryRepository;
import com.vestisen.repository.PublicationTarifRepository;
import com.vestisen.repository.UserRepository;
import com.vestisen.service.AnnonceService;
import com.vestisen.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private AnnonceService annonceService;
    
    @Autowired
    private AnnonceRepository annonceRepository;
    
    @Autowired
    private PublicationTarifRepository tarifRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/annonces")
    public ResponseEntity<Page<AnnonceDTO>> getAllAnnonces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Annonce> annonces = annonceRepository.findAll(pageable);
        return ResponseEntity.ok(annonces.map(annonceService::toDTO));
    }
    
    @PostMapping("/annonces/{id}/approve")
    public ResponseEntity<AnnonceDTO> approveAnnonce(@PathVariable Long id) {
        return ResponseEntity.ok(annonceService.approveAnnonce(id));
    }
    
    @PostMapping("/annonces/{id}/reject")
    public ResponseEntity<AnnonceDTO> rejectAnnonce(@PathVariable Long id) {
        return ResponseEntity.ok(annonceService.rejectAnnonce(id));
    }
    
    @GetMapping("/tarifs")
    public ResponseEntity<List<PublicationTarifDTO>> getTarifs() {
        List<PublicationTarif> tarifs = tarifRepository.findAll();
        List<PublicationTarifDTO> dtos = tarifs.stream().map(t -> {
            PublicationTarifDTO dto = new PublicationTarifDTO();
            dto.setId(t.getId());
            dto.setPublicationType(t.getPublicationType());
            dto.setPrice(t.getPrice());
            dto.setDurationDays(t.getDurationDays());
            dto.setActive(t.isActive());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PutMapping("/tarifs/{id}")
    public ResponseEntity<PublicationTarifDTO> updateTarif(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) Integer durationDays,
            @RequestParam(required = false) Boolean active) {
        PublicationTarif tarif = tarifRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarif not found"));
        
        if (price != null) {
            tarif.setPrice(price);
        }
        if (durationDays != null) {
            tarif.setDurationDays(durationDays);
        }
        if (active != null) {
            tarif.setActive(active);
        }
        
        PublicationTarif saved = tarifRepository.save(tarif);
        
        PublicationTarifDTO dto = new PublicationTarifDTO();
        dto.setId(saved.getId());
        dto.setPublicationType(saved.getPublicationType());
        dto.setPrice(saved.getPrice());
        dto.setDurationDays(saved.getDurationDays());
        dto.setActive(saved.isActive());
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping("/tarifs")
    public ResponseEntity<PublicationTarifDTO> createTarif(@RequestBody PublicationTarifDTO request) {
        PublicationTarif tarif = new PublicationTarif();
        tarif.setPublicationType(request.getPublicationType());
        tarif.setPrice(request.getPrice());
        tarif.setDurationDays(request.getDurationDays());
        tarif.setActive(request.isActive());
        
        PublicationTarif saved = tarifRepository.save(tarif);
        
        PublicationTarifDTO dto = new PublicationTarifDTO();
        dto.setId(saved.getId());
        dto.setPublicationType(saved.getPublicationType());
        dto.setPrice(saved.getPrice());
        dto.setDurationDays(saved.getDurationDays());
        dto.setActive(saved.isActive());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
    
    @DeleteMapping("/tarifs/{id}")
    public ResponseEntity<?> deleteTarif(@PathVariable Long id) {
        if (!tarifRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tarifRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // ========== USERS CRUD ==========
    
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAll(pageable);
        Page<UserDTO> dtos = users.map(user -> {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setPhone(user.getPhone());
            dto.setAddress(user.getAddress());
            dto.setWhatsapp(user.getWhatsapp());
            dto.setRole(user.getRole());
            dto.setEnabled(user.isEnabled());
            dto.setEmailVerified(user.isEmailVerified());
            dto.setCreatedAt(user.getCreatedAt());
            dto.setUpdatedAt(user.getUpdatedAt());
            dto.setAnnoncesCount(user.getAnnonces() != null ? user.getAnnonces().size() : 0);
            return dto;
        });
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setWhatsapp(user.getWhatsapp());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setAnnoncesCount(user.getAnnonces() != null ? user.getAnnonces().size() : 0);
        return ResponseEntity.ok(dto);
    }
    
    @PutMapping("/users/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().build();
            }
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getWhatsapp() != null) user.setWhatsapp(request.getWhatsapp());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
        if (request.getEmailVerified() != null) user.setEmailVerified(request.getEmailVerified());
        
        User saved = userRepository.save(user);
        
        UserDTO dto = new UserDTO();
        dto.setId(saved.getId());
        dto.setEmail(saved.getEmail());
        dto.setFirstName(saved.getFirstName());
        dto.setLastName(saved.getLastName());
        dto.setPhone(saved.getPhone());
        dto.setAddress(saved.getAddress());
        dto.setWhatsapp(saved.getWhatsapp());
        dto.setRole(saved.getRole());
        dto.setEnabled(saved.isEnabled());
        dto.setEmailVerified(saved.isEmailVerified());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setUpdatedAt(saved.getUpdatedAt());
        dto.setAnnoncesCount(saved.getAnnonces() != null ? saved.getAnnonces().size() : 0);
        return ResponseEntity.ok(dto);
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Ne pas permettre la suppression de l'admin
        if (user.getRole() == User.Role.ADMIN) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot delete admin user"));
        }
        
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // ========== CATEGORIES CRUD ==========
    
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        List<CategoryDTO> dtos = categories.stream().map(cat -> {
            CategoryDTO dto = new CategoryDTO();
            dto.setId(cat.getId());
            dto.setName(cat.getName());
            dto.setDescription(cat.getDescription());
            dto.setIcon(cat.getIcon());
            dto.setActive(cat.isActive());
            dto.setCreatedAt(cat.getCreatedAt());
            dto.setUpdatedAt(cat.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIcon(category.getIcon());
        dto.setActive(category.isActive());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping("/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            return ResponseEntity.badRequest().build();
        }
        
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIcon(request.getIcon());
        category.setActive(request.getActive() != null ? request.getActive() : true);
        
        Category saved = categoryRepository.save(category);
        
        CategoryDTO dto = new CategoryDTO();
        dto.setId(saved.getId());
        dto.setName(saved.getName());
        dto.setDescription(saved.getDescription());
        dto.setIcon(saved.getIcon());
        dto.setActive(saved.isActive());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setUpdatedAt(saved.getUpdatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
    
    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                return ResponseEntity.badRequest().build();
            }
            category.setName(request.getName());
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getIcon() != null) category.setIcon(request.getIcon());
        if (request.getActive() != null) category.setActive(request.getActive());
        
        Category saved = categoryRepository.save(category);
        
        CategoryDTO dto = new CategoryDTO();
        dto.setId(saved.getId());
        dto.setName(saved.getName());
        dto.setDescription(saved.getDescription());
        dto.setIcon(saved.getIcon());
        dto.setActive(saved.isActive());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setUpdatedAt(saved.getUpdatedAt());
        return ResponseEntity.ok(dto);
    }
    
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    // ========== ANNONCES CRUD (Complete) ==========
    
    @PostMapping("/annonces")
    public ResponseEntity<AnnonceDTO> createAnnonce(@Valid @RequestBody AnnonceDTO request) {
        // Implementation si nécessaire
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @PutMapping("/annonces/{id}")
    public ResponseEntity<AnnonceDTO> updateAnnonce(@PathVariable Long id, @RequestBody AnnonceDTO request) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        
        if (request.getTitle() != null) annonce.setTitle(request.getTitle());
        if (request.getDescription() != null) annonce.setDescription(request.getDescription());
        if (request.getPrice() != null) annonce.setPrice(request.getPrice());
        if (request.getCategory() != null) annonce.setCategory(request.getCategory());
        if (request.getStatus() != null) annonce.setStatus(request.getStatus());
        if (request.getSize() != null) annonce.setSize(request.getSize());
        if (request.getBrand() != null) annonce.setBrand(request.getBrand());
        if (request.getColor() != null) annonce.setColor(request.getColor());
        if (request.getLocation() != null) annonce.setLocation(request.getLocation());
        if (request.getImages() != null) annonce.setImages(request.getImages());
        
        Annonce saved = annonceRepository.save(annonce);
        return ResponseEntity.ok(annonceService.toDTO(saved));
    }
    
    @DeleteMapping("/annonces/{id}")
    public ResponseEntity<?> deleteAnnonce(@PathVariable Long id) {
        if (!annonceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        annonceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
