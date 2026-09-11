package com.devmetrics.auth;

import com.devmetrics.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

/**
 * Principal usado no SecurityContext. Carrega apenas o que os services precisam,
 * evitando uma consulta ao banco a cada chamada.
 */
public record AuthenticatedUser(Long id, String email, String displayName, String timezone, String role)
        implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getTimezone(), user.getRole().name());
    }

    public ZoneId zoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ex) {
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
