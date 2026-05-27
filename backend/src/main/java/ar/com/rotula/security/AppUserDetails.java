package ar.com.rotula.security;

import ar.com.rotula.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AppUserDetails implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final String email;
    private final String passwordHash;
    private final String role;

    public AppUserDetails(UUID id, UUID tenantId, String email, String passwordHash, String role) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static AppUserDetails from(User user) {
        return new AppUserDetails(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole()
        );
    }

    public UUID getId()       { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getRole()   { return role; }

    @Override public String getUsername()  { return email; }
    @Override public String getPassword()  { return passwordHash; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
