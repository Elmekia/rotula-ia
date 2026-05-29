package ar.com.rotula.service;

import ar.com.rotula.domain.LabelProject;
import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.DashboardSummary;
import ar.com.rotula.dto.DashboardSummary.RecentLabelEntry;
import ar.com.rotula.repository.LabelProjectRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Calcula el resumen del dashboard para el tenant del usuario autenticado.
 *
 * <p>Expone contadores agregados y los últimos 5 rótulos generados para que
 * el usuario pueda retomar su trabajo rápidamente al ingresar a la aplicación.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository      productRepository;
    private final LabelProjectRepository labelProjectRepository;

    @Transactional(readOnly = true)
    public DashboardSummary getSummary() {
        AppUserDetails user  = currentUser();
        UUID tenantId        = user.getTenantId();

        long totalProducts     = productRepository.countByTenantId(tenantId);
        long totalLabels       = labelProjectRepository.countByTenantId(tenantId);
        long pendingReview     = labelProjectRepository.countByTenantIdAndStatus(tenantId, "draft");
        long approved          = labelProjectRepository.countByTenantIdAndStatus(tenantId, "approved");

        List<RecentLabelEntry> recent = labelProjectRepository
                .findTop5ByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(lp -> toEntry(lp, tenantId))
                .toList();

        return new DashboardSummary(totalProducts, totalLabels, pendingReview, approved, recent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RecentLabelEntry toEntry(LabelProject lp, UUID tenantId) {
        String productName = productRepository.findByIdAndTenantId(lp.getProductId(), tenantId)
                .map(Product::getName)
                .orElse("—");

        return new RecentLabelEntry(
                lp.getId(),
                lp.getProductId(),
                productName,
                lp.getVersion(),
                lp.getStatus(),
                lp.getLegalDenomination(),
                lp.getCreatedAt()
        );
    }

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
