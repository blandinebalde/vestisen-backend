package com.vendit.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.vendit.config.CatalogPageLimits;
import com.vendit.dto.*;
import com.vendit.event.AnnonceApprovedEvent;
import com.vendit.model.Annonce;
import com.vendit.model.Category;
import com.vendit.model.CreditConfig;
import com.vendit.model.PlanBillingCycle;
import com.vendit.model.PublicationTarif;
import com.vendit.model.SellerPlan;
import com.vendit.model.SellerPlanCatalog;
import com.vendit.model.SellerPlanDefinition;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.CategoryRepository;
import com.vendit.repository.CreditConfigRepository;
import com.vendit.repository.CreditTransactionRepository;
import com.vendit.repository.PublicationTarifRepository;
import com.vendit.repository.SellerPlanConfigRepository;
import com.vendit.repository.UserRepository;
import com.vendit.model.SellerPlanConfig;
import com.vendit.service.ActionLogService;
import com.vendit.service.AdminStatsService;
import com.vendit.service.AdminSubscriptionStatsService;
import com.vendit.service.AnnonceService;
import com.vendit.service.SellerPlanService;
import com.vendit.service.UserService;
import com.vendit.util.PublicationTarifMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('perm:admin:full')")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private AnnonceService annonceService;
    
    @Autowired
    private AnnonceRepository annonceRepository;
    
    @Autowired
    private PublicationTarifRepository tarifRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private CreditConfigRepository creditConfigRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private ActionLogService actionLogService;

    @Autowired
    private AdminStatsService adminStatsService;

    @Autowired
    private SellerPlanService sellerPlanService;

    @Autowired
    private SellerPlanConfigRepository sellerPlanConfigRepository;

    @Autowired
    private AdminSubscriptionStatsService adminSubscriptionStatsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/annonces")
    public ResponseEntity<Page<AnnonceDTO>> getAllAnnonces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        int p = CatalogPageLimits.clampPageIndex(page);
        int s = CatalogPageLimits.clampPageSize(size);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        Annonce.Status statusFilter = parseAnnonceStatusFilter(status);
        if (statusFilter == null && status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            return ResponseEntity.badRequest().build();
        }
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Annonce> annonces = annonceRepository.findAdminFiltered(statusFilter, searchTerm, pageable);
        return ResponseEntity.ok(annonces.map(annonceService::toDTO));
    }

    private static Annonce.Status parseAnnonceStatusFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return Annonce.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Détail d'une annonce (tous statuts) pour l'admin — permet de voir les annonces en attente. */
    @GetMapping("/annonces/{publicId}")
    public ResponseEntity<AnnonceDTO> getAnnonceById(@PathVariable UUID publicId) {
        return ResponseEntity.ok(annonceService.getAnnonceDTOByPublicId(publicId));
    }
    
    @PostMapping("/annonces/{publicId}/approve")
    public ResponseEntity<AnnonceDTO> approveAnnonce(@PathVariable UUID publicId) {
        return ResponseEntity.ok(annonceService.approveAnnonce(publicId));
    }
    
    @PostMapping("/annonces/{publicId}/reject")
    public ResponseEntity<AnnonceDTO> rejectAnnonce(@PathVariable UUID publicId) {
        return ResponseEntity.ok(annonceService.rejectAnnonce(publicId));
    }
    
    @GetMapping("/tarifs")
    public ResponseEntity<Page<PublicationTarifDTO>> getTarifs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<PublicationTarif> tarifs = tarifRepository.findAll(pageable);
        return ResponseEntity.ok(tarifs.map(PublicationTarifMapper::toDto));
    }
    
    @PutMapping("/tarifs/{id}")
    public ResponseEntity<PublicationTarifDTO> updateTarif(
            @PathVariable Long id,
            @RequestParam(required = false) String typeName,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) Integer durationDays,
            @RequestParam(required = false) Boolean topPublication,
            @RequestParam(required = false) Boolean active) {
        PublicationTarif tarif = tarifRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarif not found"));
        
        if (typeName != null && !typeName.isBlank()) {
            tarif.setTypeName(typeName.trim());
        }
        if (price != null) {
            tarif.setPrice(price);
        }
        if (durationDays != null) {
            tarif.setDurationDays(PublicationTarifMapper.normalizeDurationDays(durationDays));
        }
        if (topPublication != null) {
            tarif.setTopPublication(topPublication);
        }
        if (active != null) {
            tarif.setActive(active);
        }
        
        PublicationTarif saved = tarifRepository.save(tarif);
        return ResponseEntity.ok(PublicationTarifMapper.toDto(saved));
    }
    
    @PostMapping("/tarifs")
    public ResponseEntity<PublicationTarifDTO> createTarif(@RequestBody PublicationTarifDTO request) {
        if (request.getTypeName() == null || request.getTypeName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        PublicationTarif tarif = new PublicationTarif();
        tarif.setTypeName(request.getTypeName().trim());
        tarif.setPrice(request.getPrice());
        tarif.setDurationDays(PublicationTarifMapper.normalizeDurationDays(request.getDurationDays()));
        tarif.setTopPublication(request.isTopPublication());
        tarif.setActive(request.isActive());
        
        PublicationTarif saved = tarifRepository.save(tarif);
        return ResponseEntity.status(HttpStatus.CREATED).body(PublicationTarifMapper.toDto(saved));
    }

    @GetMapping("/seller-plans")
    public ResponseEntity<List<SellerPlanConfigDTO>> getSellerPlanConfigs() {
        List<SellerPlanConfigDTO> list = sellerPlanConfigRepository.findAll(
                        Sort.by(Sort.Direction.ASC, "displayOrder"))
                .stream()
                .map(this::toSellerPlanConfigDto)
                .collect(Collectors.toList());
        if (list.isEmpty()) {
            list = SellerPlanCatalog.all().stream().map(def -> {
                SellerPlanConfigDTO dto = new SellerPlanConfigDTO();
                dto.setPlan(def.getPlan().name());
                dto.setLabel(def.getLabel());
                dto.setMonthlyPriceFcfa(def.getMonthlyPriceFcfa());
                dto.setAnnualPriceFcfa(def.annualPriceFcfa());
                dto.setCommissionPercent(def.getCommissionPercent());
                dto.setMaxActivePublications(def.getMaxActivePublications());
                dto.setUnlimitedPublications(def.isUnlimitedPublications());
                dto.setMonthlyBoostsIncluded(def.getMonthlyBoostsIncluded());
                dto.setActive(true);
                return dto;
            }).collect(Collectors.toList());
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/seller-plans/stats")
    public ResponseEntity<SubscriptionPlanStatsDTO> getSellerPlanStats() {
        return ResponseEntity.ok(adminSubscriptionStatsService.getStats());
    }

    @PutMapping("/seller-plans/{plan}")
    public ResponseEntity<SellerPlanConfigDTO> updateSellerPlanConfig(
            @PathVariable String plan,
            @RequestBody SellerPlanConfigDTO body) {
        SellerPlan planEnum;
        try {
            planEnum = SellerPlan.valueOf(plan.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        SellerPlanConfig config = sellerPlanConfigRepository.findById(planEnum)
                .orElseGet(() -> {
                    SellerPlanConfig c = new SellerPlanConfig();
                    c.setPlanCode(planEnum);
                    return c;
                });
        if (body.getLabel() != null && !body.getLabel().isBlank()) {
            config.setLabel(body.getLabel().trim());
        }
        if (body.getMonthlyPriceFcfa() != null) {
            config.setMonthlyPriceFcfa(body.getMonthlyPriceFcfa());
        }
        if (body.getCommissionPercent() != null) {
            config.setCommissionPercent(body.getCommissionPercent());
        }
        if (body.isUnlimitedPublications()) {
            config.setMaxActivePublications(-1);
        } else if (body.getMaxActivePublications() > 0) {
            config.setMaxActivePublications(body.getMaxActivePublications());
        }
        config.setMonthlyBoostsIncluded(body.getMonthlyBoostsIncluded());
        config.setActive(body.isActive());
        if (body.getDisplayOrder() > 0) {
            config.setDisplayOrder(body.getDisplayOrder());
        }
        SellerPlanConfig saved = sellerPlanConfigRepository.save(config);
        return ResponseEntity.ok(toSellerPlanConfigDto(saved));
    }

    private SellerPlanConfigDTO toSellerPlanConfigDto(SellerPlanConfig c) {
        SellerPlanDefinition def = c.toDefinition();
        SellerPlanConfigDTO dto = new SellerPlanConfigDTO();
        dto.setPlan(c.getPlanCode().name());
        dto.setLabel(c.getLabel());
        dto.setMonthlyPriceFcfa(c.getMonthlyPriceFcfa());
        dto.setAnnualPriceFcfa(def.annualPriceFcfa());
        dto.setCommissionPercent(c.getCommissionPercent());
        dto.setMaxActivePublications(c.getMaxActivePublications());
        dto.setUnlimitedPublications(c.getMaxActivePublications() < 0);
        dto.setMonthlyBoostsIncluded(c.getMonthlyBoostsIncluded());
        dto.setActive(c.isActive());
        dto.setDisplayOrder(c.getDisplayOrder());
        return dto;
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
    
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setWhatsapp(request.getWhatsapp());
        user.setRole(request.getRole() != null ? request.getRole() : User.Role.USER);
        user.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        user.setEmailVerified(request.getEmailVerified() != null ? request.getEmailVerified() : false);
        User saved = userService.createUserByAdmin(user, request.getPassword());
        UserDTO dto = new UserDTO();
        dto.setPublicId(saved.getPublicId());
        dto.setCode(saved.getCode());
        dto.setEmail(saved.getEmail());
        dto.setFirstName(saved.getFirstName());
        dto.setLastName(saved.getLastName());
        dto.setPhone(saved.getPhone());
        dto.setAddress(saved.getAddress());
        dto.setWhatsapp(saved.getWhatsapp());
        dto.setAvatarPath(saved.getAvatarPath());
        dto.setRole(saved.getRole());
        dto.setEnabled(saved.isEnabled());
        dto.setEmailVerified(saved.isEmailVerified());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setUpdatedAt(saved.getUpdatedAt());
        dto.setAnnoncesCount(0);
        dto.setCreditBalance(saved.getCreditBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String enabled,
            @RequestParam(required = false) String search) {
        int p = CatalogPageLimits.clampPageIndex(page);
        int s = CatalogPageLimits.clampPageSize(size);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        User.Role roleFilter = parseUserRoleFilter(role);
        if (roleFilter == null && role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role.trim())) {
            return ResponseEntity.badRequest().build();
        }
        Boolean enabledFilter = parseEnabledFilter(enabled);
        if (enabledFilter == null && enabled != null && !enabled.isBlank()
                && !"ALL".equalsIgnoreCase(enabled.trim())) {
            return ResponseEntity.badRequest().build();
        }
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<User> users = userRepository.findAdminFiltered(roleFilter, enabledFilter, searchTerm, pageable);
        List<Long> userIds = users.getContent().stream().map(User::getId).collect(Collectors.toList());
        Map<Long, Long> annoncesCountByUserId = safeCountAnnoncesBySellerIds(userIds);
        return ResponseEntity.ok(users.map(user -> toUserDto(user, annoncesCountByUserId)));
    }
    
    @GetMapping("/users/{publicId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        int annoncesCount = safeCountAnnoncesBySeller(user.getId());
        return ResponseEntity.ok(toUserDto(user, annoncesCount));
    }

    @GetMapping("/users/{publicId}/activity")
    public ResponseEntity<Page<ActionLogDTO>> getUserActivity(
            @PathVariable UUID publicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        int p = CatalogPageLimits.clampPageIndex(page);
        int s = CatalogPageLimits.clampPageSize(size);
        return ResponseEntity.ok(actionLogService.findByUserId(user.getId(), p, s));
    }

    @PostMapping("/users/{publicId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable UUID publicId) {
        return setUserEnabled(publicId, true, "Activation du compte");
    }

    @PostMapping("/users/{publicId}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable UUID publicId) {
        return setUserEnabled(publicId, false, "Désactivation du compte");
    }

    @PostMapping("/users/{publicId}/seller-plan")
    public ResponseEntity<?> setUserSellerPlan(
            @PathVariable UUID publicId,
            @Valid @RequestBody SellerPlanSubscribeRequest request) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != User.Role.VENDEUR && user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Plan vendeur réservé aux comptes vendeur"));
        }
        SellerPlan plan;
        try {
            plan = SellerPlan.valueOf(request.getPlan().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Plan invalide"));
        }
        PlanBillingCycle cycle = PlanBillingCycle.MONTHLY;
        if (request.getBillingCycle() != null && !request.getBillingCycle().isBlank()) {
            try {
                cycle = PlanBillingCycle.valueOf(request.getBillingCycle().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Cycle de facturation invalide"));
            }
        }
        sellerPlanService.setPlanByAdmin(user, plan, cycle);
        User saved = userRepository.findByPublicId(publicId).orElseThrow();
        return ResponseEntity.ok(toUserDto(saved, safeCountAnnoncesBySeller(saved.getId())));
    }
    
    @PutMapping("/users/{publicId}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID publicId, @RequestBody UserUpdateRequest request) {
        User user = userRepository.findByPublicId(publicId)
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
        return ResponseEntity.ok(toUserDto(saved, safeCountAnnoncesBySeller(saved.getId())));
    }

    private ResponseEntity<?> setUserEnabled(UUID publicId, boolean enabled, String actionLabel) {
        try {
            User saved = userService.setEnabledByAdmin(publicId, enabled);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminName = auth != null ? auth.getName() : "admin";
            String adminRole = auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()
                    ? auth.getAuthorities().iterator().next().getAuthority()
                    : "ADMIN";
            actionLogService.logInternalAction(
                    null,
                    adminName,
                    adminRole,
                    actionLabel + " : " + saved.getEmail(),
                    "user",
                    saved.getId(),
                    true);
            return ResponseEntity.ok(toUserDto(saved, safeCountAnnoncesBySeller(saved.getId())));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private static User.Role parseUserRoleFilter(String role) {
        if (role == null || role.isBlank() || "ALL".equalsIgnoreCase(role.trim())) {
            return null;
        }
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Boolean parseEnabledFilter(String enabled) {
        if (enabled == null || enabled.isBlank() || "ALL".equalsIgnoreCase(enabled.trim())) {
            return null;
        }
        String v = enabled.trim().toUpperCase();
        if ("TRUE".equals(v) || "ACTIVE".equals(v) || "ENABLED".equals(v)) {
            return true;
        }
        if ("FALSE".equals(v) || "DISABLED".equals(v) || "INACTIVE".equals(v)) {
            return false;
        }
        return null;
    }

    private UserDTO toUserDto(User user, int annoncesCount) {
        UserDTO dto = new UserDTO();
        dto.setPublicId(user.getPublicId());
        dto.setCode(user.getCode());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setWhatsapp(user.getWhatsapp());
        dto.setAvatarPath(user.getAvatarPath());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setEmailVerified(user.isEmailVerified());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setAnnoncesCount(annoncesCount);
        dto.setCreditBalance(user.getCreditBalance());
        if (user.getSellerPlan() != null) {
            var def = SellerPlanCatalog.get(user.getSellerPlan());
            dto.setSellerPlan(user.getSellerPlan().name());
            dto.setSellerPlanLabel(def.getLabel());
            dto.setSellerCommissionPercent(def.getCommissionPercent());
        }
        return dto;
    }

    private UserDTO toUserDto(User user, Map<Long, Long> annoncesCountByUserId) {
        return toUserDto(user, annoncesCountByUserId.getOrDefault(user.getId(), 0L).intValue());
    }
    
    @DeleteMapping("/users/{publicId}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Ne pas permettre la suppression de l'admin
        if (user.getRole() == User.Role.ADMIN) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot delete admin user"));
        }
        
        userRepository.deleteById(user.getId());
        return ResponseEntity.ok().build();
    }
    
    // ========== CATEGORIES CRUD ==========

    @GetMapping("/categories/overview")
    public ResponseEntity<AdminCategoriesOverviewDTO> getCategoriesOverview() {
        return ResponseEntity.ok(adminStatsService.getAdminCategoriesOverview());
    }

    @GetMapping("/categories")
    public ResponseEntity<Page<CategoryDTO>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String active,
            @RequestParam(required = false) String search) {
        int p = CatalogPageLimits.clampPageIndex(page);
        int s = CatalogPageLimits.clampPageSize(size);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.ASC, "name"));
        Boolean activeFilter = parseEnabledFilter(active);
        if (activeFilter == null && active != null && !active.isBlank()
                && !"ALL".equalsIgnoreCase(active.trim())) {
            return ResponseEntity.badRequest().build();
        }
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Category> categories = categoryRepository.findAdminFiltered(activeFilter, searchTerm, pageable);
        Map<Long, Long> annoncesByCategory = safeCountAnnoncesByCategoryIds();
        return ResponseEntity.ok(categories.map(cat -> toCategoryDto(cat, annoncesByCategory)));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return ResponseEntity.ok(toCategoryDto(category));
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
        return ResponseEntity.status(HttpStatus.CREATED).body(toCategoryDto(saved));
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
        return ResponseEntity.ok(toCategoryDto(saved));
    }

    @PostMapping("/categories/{id}/activate")
    public ResponseEntity<CategoryDTO> activateCategory(@PathVariable Long id) {
        return setCategoryActive(id, true);
    }

    @PostMapping("/categories/{id}/deactivate")
    public ResponseEntity<CategoryDTO> deactivateCategory(@PathVariable Long id) {
        return setCategoryActive(id, false);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<CategoryDTO> setCategoryActive(Long id, boolean active) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setActive(active);
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(toCategoryDto(saved));
    }

    private CategoryDTO toCategoryDto(Category cat) {
        return toCategoryDto(cat, safeCountAnnoncesByCategoryIds());
    }

    private CategoryDTO toCategoryDto(Category cat, Map<Long, Long> annoncesByCategory) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(cat.getId());
        dto.setName(cat.getName());
        dto.setDescription(cat.getDescription());
        dto.setIcon(cat.getIcon());
        dto.setActive(cat.isActive());
        dto.setAnnoncesCount(annoncesByCategory.getOrDefault(cat.getId(), 0L));
        dto.setCreatedAt(cat.getCreatedAt());
        dto.setUpdatedAt(cat.getUpdatedAt());
        return dto;
    }
    
    // ========== ANNONCES CRUD (admin : modification / suppression) ==========
    // Création d'annonces : POST /api/annonces (AnnonceController, rôles VENDEUR/ADMIN)

    @PutMapping("/annonces/{publicId}")
    public ResponseEntity<AnnonceDTO> updateAnnonce(@PathVariable UUID publicId, @RequestBody AnnonceDTO request) {
        Annonce annonce = annonceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        
        if (request.getTitle() != null) annonce.setTitle(request.getTitle());
        if (request.getDescription() != null) annonce.setDescription(request.getDescription());
        if (request.getPrice() != null) annonce.setPrice(request.getPrice());
        if (request.getCategoryId() != null) {
            Category cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            annonce.setCategory(cat);
        }
        if (request.getPublicationType() != null) {
            annonce.setPublicationType(request.getPublicationType());
            tarifRepository.findByTypeNameAndActiveTrue(request.getPublicationType())
                    .ifPresent(t -> annonce.setPublicationCreditCost(t.getPrice() != null ? t.getPrice() : BigDecimal.ZERO));
        }
        if (request.getStatus() != null) annonce.setStatus(request.getStatus());
        if (request.getSize() != null) annonce.setSize(request.getSize());
        if (request.getBrand() != null) annonce.setBrand(request.getBrand());
        if (request.getColor() != null) annonce.setColor(request.getColor());
        if (request.getLocation() != null) annonce.setLocation(request.getLocation());
        if (request.getImages() != null) annonce.setImages(request.getImages());
        annonce.setToutDoitPartir(request.isToutDoitPartir());
        annonce.setLot(request.isLot());
        annonce.setAcceptPaymentOnDelivery(request.isAcceptPaymentOnDelivery());
        if (request.getOriginalPrice() != null) annonce.setOriginalPrice(request.getOriginalPrice());
        if (request.getLatitude() != null) annonce.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) annonce.setLongitude(request.getLongitude());
        
        Annonce saved = annonceRepository.save(annonce);
        if (request.getStatus() == Annonce.Status.APPROVED) {
            applicationEventPublisher.publishEvent(new AnnonceApprovedEvent(this, saved));
            saved = annonceRepository.findById(saved.getId()).orElseThrow(() -> new RuntimeException("Annonce not found"));
        }
        return ResponseEntity.ok(annonceService.toDTO(saved));
    }
    
    @DeleteMapping("/annonces/{publicId}")
    public ResponseEntity<?> deleteAnnonce(@PathVariable UUID publicId) {
        Annonce annonce = annonceRepository.findByPublicId(publicId).orElse(null);
        if (annonce == null) {
            return ResponseEntity.notFound().build();
        }
        annonceRepository.deleteById(annonce.getId());
        return ResponseEntity.ok().build();
    }
    
    // ========== CREDITS CONFIG (admin) ==========
    
    @GetMapping("/credits/config")
    public ResponseEntity<CreditConfigDTO> getCreditConfig() {
        CreditConfig config = creditConfigRepository.findAll().isEmpty()
                ? null
                : creditConfigRepository.findAll().get(0);
        if (config == null) {
            config = new CreditConfig();
            config.setPricePerCreditFcfa(new BigDecimal("100"));
            config = creditConfigRepository.save(config);
        }
        CreditConfigDTO dto = new CreditConfigDTO();
        dto.setId(config.getId());
        dto.setPricePerCreditFcfa(config.getPricePerCreditFcfa());
        return ResponseEntity.ok(dto);
    }
    
    @PutMapping("/credits/config")
    public ResponseEntity<CreditConfigDTO> updateCreditConfig(@RequestBody CreditConfigDTO request) {
        CreditConfig config = creditConfigRepository.findAll().isEmpty()
                ? new CreditConfig()
                : creditConfigRepository.findAll().get(0);
        if (request.getPricePerCreditFcfa() != null && request.getPricePerCreditFcfa().compareTo(BigDecimal.ZERO) > 0) {
            config.setPricePerCreditFcfa(request.getPricePerCreditFcfa());
        }
        config = creditConfigRepository.save(config);
        CreditConfigDTO dto = new CreditConfigDTO();
        dto.setId(config.getId());
        dto.setPricePerCreditFcfa(config.getPricePerCreditFcfa());
        return ResponseEntity.ok(dto);
    }

    /** Statistiques crédits : total acheté et total dépensé. */
    @GetMapping("/stats/credits")
    public ResponseEntity<CreditStatsDTO> getCreditStats() {
        BigDecimal purchased = BigDecimal.ZERO;
        BigDecimal spent = BigDecimal.ZERO;
        try {
            BigDecimal p = creditTransactionRepository.sumCreditsPurchased();
            if (p != null) purchased = p;
        } catch (DataAccessException e) {
            logger.warn("Stats crédits (achats) indisponible — table ou schéma manquant: {}", e.getMessage());
        }
        try {
            BigDecimal s = annonceRepository.sumCreditsSpent();
            if (s != null) spent = s;
        } catch (DataAccessException e) {
            logger.warn("Stats crédits (dépenses annonces) indisponible — table ou schéma manquant: {}", e.getMessage());
        }
        return ResponseEntity.ok(new CreditStatsDTO(purchased, spent));
    }

    /** Tableau de bord : crédits et annonces par mois, année, user, catégorie, etc. */
    @GetMapping("/stats/dashboard")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminStatsService.getDashboardStats());
    }

    /** Indicateurs opérationnels pour la page tableau de bord admin. */
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewDTO> getAdminOverview() {
        return ResponseEntity.ok(adminStatsService.getAdminOverview());
    }

    // ========== LOGS (action_logs) ==========

    @GetMapping("/logs")
    public ResponseEntity<Page<ActionLogDTO>> getLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actionLabel,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ActionLogFilterRequest filter = new ActionLogFilterRequest();
        filter.setSearch(search);
        filter.setUsername(username);
        filter.setUserRole(userRole);
        filter.setResourceType(resourceType);
        filter.setActionLabel(actionLabel);
        if (dateFrom != null && !dateFrom.isBlank()) {
            try {
                filter.setDateFrom(LocalDate.parse(dateFrom, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception ignored) { }
        }
        if (dateTo != null && !dateTo.isBlank()) {
            try {
                filter.setDateTo(LocalDate.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception ignored) { }
        }
        filter.setSuccess(success);
        filter.setHttpMethod(httpMethod);
        filter.setPage(page);
        filter.setSize(size);
        return ResponseEntity.ok(actionLogService.search(filter));
    }

    @GetMapping(value = "/logs/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportLogsExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actionLabel,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String httpMethod) {
        ActionLogFilterRequest filter = new ActionLogFilterRequest();
        filter.setSearch(search);
        filter.setUsername(username);
        filter.setUserRole(userRole);
        filter.setResourceType(resourceType);
        filter.setActionLabel(actionLabel);
        if (dateFrom != null && !dateFrom.isBlank()) {
            try {
                filter.setDateFrom(LocalDate.parse(dateFrom, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception ignored) { }
        }
        if (dateTo != null && !dateTo.isBlank()) {
            try {
                filter.setDateTo(LocalDate.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception ignored) { }
        }
        filter.setSuccess(success);
        filter.setHttpMethod(httpMethod);
        byte[] bytes = actionLogService.exportToExcel(filter);
        String filename = "logs_actions_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".xlsx";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    /** Si la table annonces n'existe pas (schéma partiel), évite une 500 sur la liste utilisateurs. */
    private Map<Long, Long> safeCountAnnoncesBySellerIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        try {
            return annonceRepository.countBySellerIdIn(userIds).stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        } catch (DataAccessException e) {
            logger.warn("Comptage annonces par vendeur impossible (table absente ou schéma incomplet): {}", e.getMessage());
            return Map.of();
        }
    }

    private int safeCountAnnoncesBySeller(Long userId) {
        try {
            return (int) annonceRepository.countBySeller_Id(userId);
        } catch (DataAccessException e) {
            logger.warn("Comptage annonces pour l'utilisateur {} impossible: {}", userId, e.getMessage());
            return 0;
        }
    }

    private Map<Long, Long> safeCountAnnoncesByCategoryIds() {
        try {
            Map<Long, Long> map = new HashMap<>();
            for (Object[] row : annonceRepository.countAnnoncesByCategory()) {
                map.put(((Number) row[0]).longValue(), ((Number) row[2]).longValue());
            }
            return map;
        } catch (DataAccessException e) {
            logger.warn("Comptage annonces par catégorie impossible: {}", e.getMessage());
            return Map.of();
        }
    }
}
