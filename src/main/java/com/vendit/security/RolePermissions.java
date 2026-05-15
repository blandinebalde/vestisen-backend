package com.vendit.security;

import com.vendit.model.User;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Associe chaque {@link User.Role} aux {@link Permission} accordées au chargement utilisateur.
 */
public final class RolePermissions {

    private static final Set<Permission> USER_PERMS = Collections.unmodifiableSet(EnumSet.of(
            Permission.MARKET_CONTACT,
            Permission.MARKET_BUY,
            Permission.CREDIT_BALANCE_READ,
            Permission.CART_USE,
            Permission.CONVERSATION_USE,
            Permission.REVIEW_READ,
            Permission.REVIEW_WRITE,
            Permission.PAYMENT_USE,
            Permission.PROFILE_ACCESS
    ));

    private static final Set<Permission> VENDEUR_PERMS;

    private static final Map<User.Role, Set<Permission>> BY_ROLE;

    static {
        EnumSet<Permission> vendeur = EnumSet.copyOf(USER_PERMS);
        vendeur.add(Permission.ANNONCE_CREATE);
        vendeur.add(Permission.ANNONCE_SELLER_READ);
        vendeur.add(Permission.ANNONCE_SELLER_WRITE);
        vendeur.add(Permission.CREDIT_VENDOR);
        VENDEUR_PERMS = Collections.unmodifiableSet(vendeur);

        Map<User.Role, Set<Permission>> m = new EnumMap<>(User.Role.class);
        m.put(User.Role.USER, USER_PERMS);
        m.put(User.Role.VENDEUR, VENDEUR_PERMS);
        m.put(User.Role.ADMIN, Collections.unmodifiableSet(EnumSet.allOf(Permission.class)));
        BY_ROLE = Collections.unmodifiableMap(m);
    }

    private RolePermissions() {
    }

    public static Set<Permission> forRole(User.Role role) {
        if (role == null) {
            return USER_PERMS;
        }
        return BY_ROLE.getOrDefault(role, USER_PERMS);
    }
}
