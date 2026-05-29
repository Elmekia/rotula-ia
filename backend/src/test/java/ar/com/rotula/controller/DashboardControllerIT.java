package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.dto.DashboardSummary;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerIT {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DashboardService         dashboardService;
    @MockBean JwtService               jwtService;
    @MockBean TenantDataSourceWrapper  tenantDataSourceWrapper;

    private static final UUID LABEL_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // ── GET /dashboard/summary ────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getSummary_retorna_200_con_contadores() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summaryConEntradas());

        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(10))
                .andExpect(jsonPath("$.totalLabels").value(25))
                .andExpect(jsonPath("$.pendingReviewLabels").value(5))
                .andExpect(jsonPath("$.approvedLabels").value(15))
                .andExpect(jsonPath("$.recentLabels").isArray())
                .andExpect(jsonPath("$.recentLabels.length()").value(1));
    }

    @Test
    @WithMockUser
    void getSummary_recentLabels_contiene_campos_esperados() throws Exception {
        when(dashboardService.getSummary()).thenReturn(summaryConEntradas());

        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentLabels[0].labelId").value(LABEL_ID.toString()))
                .andExpect(jsonPath("$.recentLabels[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.recentLabels[0].productName").value("Galletita Cracker"))
                .andExpect(jsonPath("$.recentLabels[0].version").value(2))
                .andExpect(jsonPath("$.recentLabels[0].status").value("draft"))
                .andExpect(jsonPath("$.recentLabels[0].legalDenomination").value("Galletita"));
    }

    @Test
    @WithMockUser
    void getSummary_sin_rotulos_retorna_lista_vacia() throws Exception {
        DashboardSummary empty = new DashboardSummary(0L, 0L, 0L, 0L, List.of());
        when(dashboardService.getSummary()).thenReturn(empty);

        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(0))
                .andExpect(jsonPath("$.recentLabels").isEmpty());
    }

    @Test
    void getSummary_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DashboardSummary summaryConEntradas() {
        DashboardSummary.RecentLabelEntry entry = new DashboardSummary.RecentLabelEntry(
                LABEL_ID,
                PRODUCT_ID,
                "Galletita Cracker",
                2,
                "draft",
                "Galletita",
                OffsetDateTime.now()
        );
        return new DashboardSummary(10L, 25L, 5L, 15L, List.of(entry));
    }
}
