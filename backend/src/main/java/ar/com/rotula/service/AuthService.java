package ar.com.rotula.service;

import ar.com.rotula.domain.Tenant;
import ar.com.rotula.domain.User;
import ar.com.rotula.dto.AuthResponse;
import ar.com.rotula.dto.LoginRequest;
import ar.com.rotula.dto.RefreshRequest;
import ar.com.rotula.dto.RegisterRequest;
import ar.com.rotula.exception.AuthException;
import ar.com.rotula.exception.EmailAlreadyExistsException;
import ar.com.rotula.repository.TenantRepository;
import ar.com.rotula.repository.UserRepository;
import ar.com.rotula.security.AppUserDetails;
import ar.com.rotula.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository    userRepository;
    private final TenantRepository  tenantRepository;
    private final JwtService        jwtService;
    private final PasswordEncoder   passwordEncoder;
    private final JdbcTemplate      jdbc;

    /**
     * Crea un nuevo Tenant y su usuario owner.
     * Desactiva RLS en la transacción para poder verificar unicidad del email
     * y para insertar el usuario (que tiene FORCE ROW LEVEL SECURITY).
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        jdbc.execute("SET LOCAL row_security = off");

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(req.companyName())
                .plan("starter")
                .status("active")
                .build());

        User user = userRepository.save(User.builder()
                .tenantId(tenant.getId())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role("owner")
                .status("active")
                .build());

        return buildAuthResponse(AppUserDetails.from(user));
    }

    /**
     * Autentica con email y contraseña.
     * Desactiva RLS para poder buscar el usuario sin conocer su tenant_id de antemano.
     */
    @Transactional
    public AuthResponse login(LoginRequest req) {
        jdbc.execute("SET LOCAL row_security = off");

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new AuthException("Credenciales inválidas"));

        if (!"active".equals(user.getStatus())) {
            throw new AuthException("Cuenta inactiva o suspendida");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthException("Credenciales inválidas");
        }

        return buildAuthResponse(AppUserDetails.from(user));
    }

    /**
     * Genera un nuevo access token a partir de un refresh token válido.
     * Usa el tenant_id del refresh token para configurar el contexto RLS
     * y verificar que el usuario sigue activo.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        String refreshToken = req.refreshToken();

        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new AuthException("Token de refresco inválido o expirado");
        }

        Claims claims   = jwtService.extractAllClaims(refreshToken);
        UUID userId     = UUID.fromString(claims.getSubject());
        UUID tenantId   = UUID.fromString(claims.get("tenant_id", String.class));

        // UUID.toString() es seguro (solo hex + guiones), no hay riesgo de inyección
        jdbc.execute("SET LOCAL app.current_tenant = '" + tenantId + "'");

        User user = userRepository.findById(userId)
                .filter(u -> "active".equals(u.getStatus()))
                .orElseThrow(() -> new AuthException("Usuario no encontrado o inactivo"));

        AppUserDetails userDetails = AppUserDetails.from(user);
        return new AuthResponse(
                jwtService.generateAccessToken(userDetails),
                refreshToken,   // no se rota el refresh token en esta versión
                "Bearer",
                jwtService.getAccessTokenExpirySeconds()
        );
    }

    private AuthResponse buildAuthResponse(AppUserDetails userDetails) {
        return new AuthResponse(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                "Bearer",
                jwtService.getAccessTokenExpirySeconds()
        );
    }
}
