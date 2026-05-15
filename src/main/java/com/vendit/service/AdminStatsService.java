package com.vendit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.vendit.dto.*;
import com.vendit.model.Annonce;
import com.vendit.model.CreditTransaction;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.CategoryRepository;
import com.vendit.repository.CreditTransactionRepository;
import com.vendit.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminStatsService {

    private static final Logger log = LoggerFactory.getLogger(AdminStatsService.class);

    private static final String[] MONTH_LABELS = {"Janv.", "Févr.", "Mars", "Avr.", "Mai", "Juin", "Juil.", "Août", "Sept.", "Oct.", "Nov.", "Déc."};

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;
    @Autowired
    private AnnonceRepository annonceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    /** Vue synthèse pour le tableau de bord admin (modération, comptes, crédits, engagement). */
    public AdminOverviewDTO getAdminOverview() {
        List<StatsAnnoncesByStatusDTO> byStatus = buildAnnoncesByStatus();
        long pending = countStatus(byStatus, "PENDING");
        long approved = countStatus(byStatus, "APPROVED");
        long rejected = countStatus(byStatus, "REJECTED");
        long sold = countStatus(byStatus, "SOLD");
        long expired = countStatus(byStatus, "EXPIRED");
        long total = pending + approved + rejected + sold + expired;

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        long pendingOld = safeLong(() -> annonceRepository.countByStatusAndCreatedAtBefore(
                Annonce.Status.PENDING, sevenDaysAgo));
        long createdThisMonth = safeLong(() -> annonceRepository.countByCreatedAtSince(monthStart));

        long usersTotal = safeLong(userRepository::count);
        Map<User.Role, Long> roleCounts = countUsersByRole();
        long vendeurs = roleCounts.getOrDefault(User.Role.VENDEUR, 0L);
        long clients = roleCounts.getOrDefault(User.Role.USER, 0L);
        long admins = roleCounts.getOrDefault(User.Role.ADMIN, 0L);
        long disabled = safeLong(userRepository::countByEnabledFalse);
        long emailUnverified = safeLong(userRepository::countByEmailVerifiedFalse);

        BigDecimal creditsPurchased = safeDecimal(creditTransactionRepository::sumCreditsPurchased);
        BigDecimal creditsSpent = safeDecimal(annonceRepository::sumCreditsSpent);
        BigDecimal revenueTotal = safeDecimal(creditTransactionRepository::sumRevenueFcfaCompleted);
        BigDecimal revenueMonth = safeDecimal(() -> creditTransactionRepository.sumRevenueFcfaCompletedSince(monthStart));
        long pendingTx = safeLong(() -> creditTransactionRepository.countByStatus(CreditTransaction.Status.PENDING));

        long engagementApproved = approved;
        long totalViews = 0;
        long totalContacts = 0;
        List<Object[]> engagementRows = safeList(annonceRepository::sumViewsAndContactsApproved);
        if (!engagementRows.isEmpty() && engagementRows.get(0).length >= 3) {
            engagementApproved = ((Number) engagementRows.get(0)[0]).longValue();
            totalViews = ((Number) engagementRows.get(0)[1]).longValue();
            totalContacts = ((Number) engagementRows.get(0)[2]).longValue();
        }
        double contactRate = totalViews > 0
                ? Math.round((totalContacts * 1000.0) / totalViews) / 10.0
                : 0.0;

        List<StatsAnnoncesByCategoryDTO> topCategories = buildAnnoncesByCategory().stream()
                .limit(5)
                .collect(Collectors.toList());

        List<AdminPendingAnnonceDTO> oldestPending = loadOldestPendingAnnonces();

        return new AdminOverviewDTO(
                pending,
                pendingOld,
                approved,
                rejected,
                sold,
                expired,
                total,
                createdThisMonth,
                usersTotal,
                vendeurs,
                clients,
                admins,
                disabled,
                emailUnverified,
                creditsPurchased,
                creditsSpent,
                revenueTotal,
                revenueMonth,
                pendingTx,
                engagementApproved,
                totalViews,
                totalContacts,
                contactRate,
                topCategories,
                oldestPending
        );
    }

    /** Synthèse pour l'onglet admin catégories. */
    public AdminCategoriesOverviewDTO getAdminCategoriesOverview() {
        long total = safeLong(categoryRepository::count);
        long active = safeLong(categoryRepository::countByActiveTrue);
        long inactive = safeLong(categoryRepository::countByActiveFalse);
        List<StatsAnnoncesByCategoryDTO> top = buildAnnoncesByCategory().stream()
                .limit(8)
                .collect(Collectors.toList());
        List<Object[]> allCatRows = safeList(annonceRepository::countAnnoncesByCategory);
        long sumFromDb = 0;
        for (Object[] row : allCatRows) {
            sumFromDb += ((Number) row[2]).longValue();
        }
        return new AdminCategoriesOverviewDTO(total, active, inactive, sumFromDb, top);
    }

    private List<AdminPendingAnnonceDTO> loadOldestPendingAnnonces() {
        try {
            return annonceRepository
                    .findByStatusOrderByCreatedAtAsc(Annonce.Status.PENDING, PageRequest.of(0, 8))
                    .stream()
                    .map(this::toPendingDto)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.warn("File d'attente modération indisponible: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private AdminPendingAnnonceDTO toPendingDto(Annonce a) {
        String email = a.getSeller() != null ? a.getSeller().getEmail() : null;
        String cat = a.getCategory() != null ? a.getCategory().getName() : null;
        return new AdminPendingAnnonceDTO(
                a.getPublicId(),
                a.getTitle(),
                email,
                cat,
                a.getPublicationType(),
                a.getCreatedAt()
        );
    }

    private Map<User.Role, Long> countUsersByRole() {
        Map<User.Role, Long> map = new EnumMap<>(User.Role.class);
        for (User.Role r : User.Role.values()) {
            map.put(r, 0L);
        }
        for (Object[] row : safeList(userRepository::countByRole)) {
            User.Role role = (User.Role) row[0];
            long count = ((Number) row[1]).longValue();
            map.put(role, count);
        }
        return map;
    }

    private static long countStatus(List<StatsAnnoncesByStatusDTO> list, String status) {
        return list.stream()
                .filter(s -> status.equals(s.getStatus()))
                .mapToLong(StatsAnnoncesByStatusDTO::getCount)
                .sum();
    }

    /** JPQL renvoie Annonce.Status ; requêtes natives renvoient souvent une String. */
    private static String annonceStatusName(Object status) {
        if (status == null) {
            return "";
        }
        if (status instanceof Annonce.Status s) {
            return s.name();
        }
        return status.toString();
    }

    private long safeLong(java.util.function.Supplier<Long> supplier) {
        try {
            Long v = supplier.get();
            return v != null ? v : 0L;
        } catch (DataAccessException e) {
            log.warn("Compteur admin ignoré — schéma incomplet: {}", e.getMessage());
            return 0L;
        }
    }

    public DashboardStatsDTO getDashboardStats() {
        BigDecimal purchased = safeDecimal(creditTransactionRepository::sumCreditsPurchased);
        BigDecimal spent = safeDecimal(annonceRepository::sumCreditsSpent);

        List<StatsCreditsByMonthDTO> creditsByMonth = buildCreditsByMonth();
        List<StatsCreditsByYearDTO> creditsByYear = buildCreditsByYear();
        List<StatsCreditsByUserDTO> creditsByUser = buildCreditsByUser();
        List<StatsAnnoncesByMonthDTO> annoncesByMonth = buildAnnoncesByMonthFromCounts();
        List<StatsAnnoncesByYearDTO> annoncesByYear = buildAnnoncesByYear();
        List<StatsAnnoncesByCategoryDTO> annoncesByCategory = buildAnnoncesByCategory();
        List<StatsAnnoncesByStatusDTO> annoncesByStatus = buildAnnoncesByStatus();
        StatsEngagementDTO engagement = buildEngagement();

        return new DashboardStatsDTO(
                purchased, spent,
                creditsByMonth, creditsByYear, creditsByUser,
                annoncesByMonth, annoncesByYear, annoncesByCategory, annoncesByStatus,
                engagement
        );
    }

    private List<StatsCreditsByMonthDTO> buildCreditsByMonth() {
        List<Object[]> purchasedRows = safeList(creditTransactionRepository::sumCreditsPurchasedByMonth);
        List<Object[]> spentRows = safeList(annonceRepository::sumCreditsSpentByMonth);
        Map<YearMonth, BigDecimal> purchasedMap = toYearMonthMap(purchasedRows, 2);
        Map<YearMonth, BigDecimal> spentMap = toYearMonthMap(spentRows, 2);
        List<StatsCreditsByMonthDTO> result = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(11);
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            int y = ym.getYear();
            int m = ym.getMonthValue();
            String label = MONTH_LABELS[m - 1] + " " + y;
            BigDecimal p = purchasedMap.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal s = spentMap.getOrDefault(ym, BigDecimal.ZERO);
            result.add(new StatsCreditsByMonthDTO(y, m, label, p, s));
        }
        return result;
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        return BigDecimal.ZERO;
    }

    private Map<YearMonth, BigDecimal> toYearMonthMap(List<Object[]> rows, int valueIndex) {
        Map<YearMonth, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            map.put(YearMonth.of(year, month), toDecimal(row.length > valueIndex ? row[valueIndex] : null));
        }
        return map;
    }

    private List<StatsCreditsByYearDTO> buildCreditsByYear() {
        List<Object[]> purchasedRows = safeList(creditTransactionRepository::sumCreditsPurchasedByYear);
        List<Object[]> spentRows = safeList(annonceRepository::sumCreditsSpentByYear);
        int currentYear = LocalDate.now().getYear();
        Map<Integer, BigDecimal> purchasedMap = new HashMap<>();
        Map<Integer, BigDecimal> spentMap = new HashMap<>();
        for (int y = currentYear - 4; y <= currentYear; y++) {
            purchasedMap.put(y, BigDecimal.ZERO);
            spentMap.put(y, BigDecimal.ZERO);
        }
        for (Object[] row : purchasedRows) {
            int year = ((Number) row[0]).intValue();
            if (year >= currentYear - 4)             purchasedMap.put(year, toDecimal(row[1]));
        }
        for (Object[] row : spentRows) {
            int year = ((Number) row[0]).intValue();
            if (year >= currentYear - 4) spentMap.put(year, toDecimal(row[1]));
        }
        List<StatsCreditsByYearDTO> result = new ArrayList<>();
        for (int y = currentYear - 4; y <= currentYear; y++) {
            result.add(new StatsCreditsByYearDTO(y, purchasedMap.get(y), spentMap.get(y)));
        }
        return result;
    }

    private List<StatsCreditsByUserDTO> buildCreditsByUser() {
        List<Object[]> rows = safeList(creditTransactionRepository::sumCreditsPurchasedByUserTop10);
        return rows.stream().map(row -> {
            Long userId = ((Number) row[0]).longValue();
            String email = userRepository.findById(userId).map(User::getEmail).orElse("?");
            BigDecimal credits = toDecimal(row[1]);
            return new StatsCreditsByUserDTO(userId, email, credits);
        }).collect(Collectors.toList());
    }

    private List<StatsAnnoncesByMonthDTO> buildAnnoncesByMonthFromCounts() {
        List<Object[]> byStatus = safeList(annonceRepository::countAnnoncesByMonthAndStatus);
        YearMonth start = YearMonth.now().minusMonths(11);
        Map<YearMonth, long[]> map = new HashMap<>();
        for (int i = 0; i < 12; i++) map.put(start.plusMonths(i), new long[]{0, 0, 0});
        for (Object[] row : byStatus) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            String status = annonceStatusName(row[2]);
            long count = ((Number) row[3]).longValue();
            YearMonth ym = YearMonth.of(year, month);
            if (map.containsKey(ym)) {
                long[] c = map.get(ym);
                c[0] += count; // total créées ce mois
                if ("APPROVED".equals(status)) c[1] += count;
                else if ("SOLD".equals(status)) c[2] += count;
            }
        }
        List<StatsAnnoncesByMonthDTO> result = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = start.plusMonths(i);
            long[] c = map.get(ym);
            result.add(new StatsAnnoncesByMonthDTO(ym.getYear(), ym.getMonthValue(),
                    MONTH_LABELS[ym.getMonthValue() - 1] + " " + ym.getYear(), c[0], c[1], c[2]));
        }
        return result;
    }

    private List<StatsAnnoncesByYearDTO> buildAnnoncesByYear() {
        List<Object[]> rows = safeList(annonceRepository::countAnnoncesByYearAndStatus);
        int currentYear = LocalDate.now().getYear();
        Map<Integer, long[]> map = new HashMap<>();
        for (int y = currentYear - 4; y <= currentYear; y++) map.put(y, new long[]{0, 0, 0});
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            String status = annonceStatusName(row[1]);
            long count = ((Number) row[2]).longValue();
            if (map.containsKey(year)) {
                long[] c = map.get(year);
                if ("PENDING".equals(status)) c[0] += count;
                else if ("APPROVED".equals(status)) c[1] += count;
                else if ("SOLD".equals(status)) c[2] += count;
            }
        }
        List<StatsAnnoncesByYearDTO> result = new ArrayList<>();
        for (int y = currentYear - 4; y <= currentYear; y++) {
            long[] c = map.get(y);
            result.add(new StatsAnnoncesByYearDTO(y, c[0], c[1], c[2]));
        }
        return result;
    }

    private List<StatsAnnoncesByCategoryDTO> buildAnnoncesByCategory() {
        return safeList(annonceRepository::countAnnoncesByCategory).stream()
                .map(row -> new StatsAnnoncesByCategoryDTO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());
    }

    private List<StatsAnnoncesByStatusDTO> buildAnnoncesByStatus() {
        return safeList(annonceRepository::countAnnoncesByStatus).stream()
                .map(row -> new StatsAnnoncesByStatusDTO(
                        annonceStatusName(row[0]),
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    private StatsEngagementDTO buildEngagement() {
        List<Object[]> row = safeList(annonceRepository::sumViewsAndContacts);
        long total = 0, views = 0, contacts = 0;
        if (!row.isEmpty() && row.get(0).length >= 3) {
            total = ((Number) row.get(0)[0]).longValue();
            views = ((Number) row.get(0)[1]).longValue();
            contacts = ((Number) row.get(0)[2]).longValue();
        }
        double avgV = total > 0 ? (double) views / total : 0;
        double avgC = total > 0 ? (double) contacts / total : 0;
        return new StatsEngagementDTO(total, views, contacts, avgV, avgC);
    }

    private BigDecimal safeDecimal(java.util.function.Supplier<BigDecimal> supplier) {
        try {
            BigDecimal v = supplier.get();
            return v != null ? v : BigDecimal.ZERO;
        } catch (DataAccessException e) {
            log.warn("Stats (agrégat décimal) ignorée — schéma incomplet ou table absente: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private List<Object[]> safeList(java.util.function.Supplier<List<Object[]>> supplier) {
        try {
            List<Object[]> v = supplier.get();
            return v != null ? v : Collections.emptyList();
        } catch (DataAccessException e) {
            log.warn("Stats (requête liste) ignorée — schéma incomplet ou table absente: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
