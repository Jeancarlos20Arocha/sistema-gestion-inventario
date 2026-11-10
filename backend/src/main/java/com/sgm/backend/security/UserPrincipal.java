package com.sgm.backend.security;

import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal autenticado construido a partir de los claims del JWT.
 * <p>
 * {@code userId} y {@code username} se certifican no nulos en
 * {@link JwtAuthenticationFilter} antes de crear la instancia; {@code role} y
 * {@code machineId} son opcionales según el token de origen.
 */
public record UserPrincipal(
        @NonNull Long userId,
        @NonNull String username,
        String role,
        String machineId,
        boolean enabled,
        boolean accountLocked) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}