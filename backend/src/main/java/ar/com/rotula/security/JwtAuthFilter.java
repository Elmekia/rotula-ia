package ar.com.rotula.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Valida el JWT de cada request. Si es válido, construye el AppUserDetails
 * directamente desde los claims (sin consulta a DB) y setea el SecurityContext.
 * También registra el tenant_id en TenantContextHolder para que TenantAwareDataSource
 * pueda aplicar el SET app.current_tenant en la próxima operación de DB.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            if (jwtService.isAccessToken(token) && jwtService.isTokenValid(token)) {
                Claims claims = jwtService.extractAllClaims(token);

                UUID userId   = UUID.fromString(claims.getSubject());
                UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));
                String email  = claims.get("email", String.class);
                String role   = claims.get("role",  String.class);

                // Debe setearse antes de cualquier operación de DB para que
                // TenantAwareDataSource pueda configurar app.current_tenant
                TenantContextHolder.setTenantId(tenantId);

                AppUserDetails userDetails = new AppUserDetails(userId, tenantId, email, null, role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
