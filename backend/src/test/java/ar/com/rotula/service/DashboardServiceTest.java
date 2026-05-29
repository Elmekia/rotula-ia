package ar.com.rotula.service;

import ar.com.rotula.domain.LabelProject;
import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.DashboardSummary;
import ar.com.rotula.repository.LabelProjectRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock ProductRepository      productRepository;
    @Mock LabelProjectRepository labelProjectRepository;

    @InjectMocks DashboardService service;

    private static final UUID TENANT_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID    = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PRODUCT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void authenticate() {
        AppUserDetails user = new AppUserDetails(USER_ID, TENANT_ID, "u@test.com", "hash", "user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // ── getSummary() ──────────────────────────────────────────────────────────

    @Test
    void getSummary_retorna_contadores_correctos() {
        when(productRepository.countByTenantId(TENANT_ID)).thenReturn(12L);
        when(labelProjectRepository.countByTenantId(TENANT_ID)).thenReturn(35L);
        when(labelProjectRepository.countByTenantIdAndStatus(TENANT_ID, "draft")).thenReturn(7L);
        when(labelProjectRepository.countByTenantIdAndStatus(TENANT_ID, "approved")).thenReturn(20L);
        when(labelProjectRepository.findTop5ByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of());

        DashboardSummary summary = service.getSummary();

        assertThat(summary.totalProducts()).isEqualTo(12L);
        assertThat(summary.totalLabels()).isEqualTo(35L);
        assertThat(summary.pendingReviewLabels()).isEqualTo(7L);
        assertThat(summary.approvedLabels()).isEqualTo(20L);
        assertThat(summary.recentLabels()).isEmpty();
    }

    @Test
    void getSummary_recentLabels_contiene_nombre_del_producto() {
        LabelProject lp = buildLabelProject(UUID.randomUUID(), PRODUCT_ID, 1, "draft", "Galletita Cracker");
        Product product = Product.builder()
                .id(PRODUCT_ID).tenantId(TENANT_ID)
                .name("Galletita Cracker").category("panificados")
                .status("active")
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();

        when(productRepository.countByTenantId(TENANT_ID)).thenReturn(1L);
        when(labelProjectRepository.countByTenantId(TENANT_ID)).thenReturn(1L);
        when(labelProjectRepository.countByTenantIdAndStatus(any(), any())).thenReturn(0L);
        when(labelProjectRepository.findTop5ByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of(lp));
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(product));

        DashboardSummary summary = service.getSummary();

        assertThat(summary.recentLabels()).hasSize(1);
        DashboardSummary.RecentLabelEntry entry = summary.recentLabels().get(0);
        assertThat(entry.productName()).isEqualTo("Galletita Cracker");
        assertThat(entry.productId()).isEqualTo(PRODUCT_ID);
        assertThat(entry.version()).isEqualTo(1);
        assertThat(entry.status()).isEqualTo("draft");
        assertThat(entry.legalDenomination()).isEqualTo("Galletita Cracker");
    }

    @Test
    void getSummary_recentLabels_productName_fallback_cuando_no_existe_producto() {
        UUID labelId = UUID.randomUUID();
        LabelProject lp = buildLabelProject(labelId, PRODUCT_ID, 1, "draft", "Leche Entera");

        when(productRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
        when(labelProjectRepository.countByTenantId(TENANT_ID)).thenReturn(1L);
        when(labelProjectRepository.countByTenantIdAndStatus(any(), any())).thenReturn(0L);
        when(labelProjectRepository.findTop5ByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of(lp));
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());  // producto eliminado

        DashboardSummary summary = service.getSummary();

        assertThat(summary.recentLabels()).hasSize(1);
        assertThat(summary.recentLabels().get(0).productName()).isEqualTo("—");
    }

    @Test
    void getSummary_recentLabels_hasta_5_entradas() {
        List<LabelProject> five = List.of(
                buildLabelProject(UUID.randomUUID(), PRODUCT_ID, 5, "draft", "P"),
                buildLabelProject(UUID.randomUUID(), PRODUCT_ID, 4, "draft", "P"),
                buildLabelProject(UUID.randomUUID(), PRODUCT_ID, 3, "draft", "P"),
                buildLabelProject(UUID.randomUUID(), PRODUCT_ID, 2, "approved", "P"),
                buildLabelProject(UUID.randomUUID(), PRODUCT_ID, 1, "approved", "P")
        );
        Product product = Product.builder()
                .id(PRODUCT_ID).tenantId(TENANT_ID).name("Prod")
                .category("panificados").status("active")
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();

        when(productRepository.countByTenantId(any())).thenReturn(1L);
        when(labelProjectRepository.countByTenantId(any())).thenReturn(5L);
        when(labelProjectRepository.countByTenantIdAndStatus(any(), any())).thenReturn(0L);
        when(labelProjectRepository.findTop5ByTenantIdOrderByCreatedAtDesc(TENANT_ID)).thenReturn(five);
        when(productRepository.findByIdAndTenantId(any(), any())).thenReturn(Optional.of(product));

        DashboardSummary summary = service.getSummary();

        assertThat(summary.recentLabels()).hasSize(5);
        assertThat(summary.recentLabels().get(0).version()).isEqualTo(5);
        assertThat(summary.recentLabels().get(4).version()).isEqualTo(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LabelProject buildLabelProject(UUID id, UUID productId, int version,
                                           String status, String legalDen) {
        LabelProject lp = new LabelProject();
        lp.setId(id);
        lp.setProductId(productId);
        lp.setTenantId(TENANT_ID);
        lp.setVersion(version);
        lp.setStatus(status);
        lp.setLegalDenomination(legalDen);
        lp.setCreatedAt(OffsetDateTime.now());
        lp.setUpdatedAt(OffsetDateTime.now());
        return lp;
    }
}
