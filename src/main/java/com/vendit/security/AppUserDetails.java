package com.vendit.security;

import com.vendit.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * {@link UserDetails} enrichi pour JWT : version de jeton (invalidation globale) et id utilisateur.
 */
public final class AppUserDetails implements UserDetails {

    private final String username;
    private final String password;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final long tokenVersion;
    private final Long userId;

    public AppUserDetails(
            String username,
            String password,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities,
            long tokenVersion,
            Long userId) {
        this.username = username;
        this.password = password;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
        this.authorities = authorities;
        this.tokenVersion = tokenVersion;
        this.userId = userId;
    }

    /**
     * Aligné sur l'ancien {@code org.springframework.security.core.userdetails.User} :
     * {@code enabled} = compte actif ; {@code credentialsNonExpired} = email vérifié (ou admin).
     */
    public static AppUserDetails fromUser(User user, Collection<? extends GrantedAuthority> authorities) {
        boolean accountEnabled = user.isEmailVerified() || user.getRole() == User.Role.ADMIN;
        return new AppUserDetails(
                user.getEmail(),
                user.getPassword(),
                true,
                true,
                accountEnabled,
                user.isEnabled(),
                authorities,
                user.getTokenVersion(),
                user.getId());
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
