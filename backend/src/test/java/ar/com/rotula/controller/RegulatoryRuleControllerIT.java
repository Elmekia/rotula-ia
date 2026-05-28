package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.dto.RegulatoryRuleResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.RegulatoryRuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegulatoryRuleController.class)
class RegulatoryRuleControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RegulatoryRuleService ruleService;
    @MockBean JwtService jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private RegulatoryRuleResponse sampleResponse(String code, String category) {
        return new RegulatoryRuleResponse(
                UUID.randomUUID(),
                code,
                category,
                "Regla de prueba: " + code,
                "{\"category\":[\"" + category + "\"]}",
                "DECLARAR_LEYENDA_OBLIGATORIA",
                "Leyenda de prueba",
                "CAA Art. 1",
                LocalDate.of(2023, 1, 1),
                null,
                1,
                "Notas de prueba",
                OffsetDateTime.now()
        );
    }

    // ── GET /regulatory-rules ────────────────────────────────────────────────

    @Test
    @WithMockUser
    void list_retorna_todas_las_reglas_activas() throws Exception {
        when(ruleService.findActive(null)).thenReturn(List.of(
                sampleResponse("BEB-001", "bebidas"),
                sampleResponse("SNK-001", "snacks")
        ));

        mockMvc.perform(get("/regulatory-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("BEB-001"))
                .andExpect(jsonPath("$[1].code").value("SNK-001"));
    }

    @Test
    @WithMockUser
    void list_con_categoria_filtra_resultados() throws Exception {
        when(ruleService.findActive("bebidas")).thenReturn(
                List.of(sampleResponse("BEB-001", "bebidas"))
        );

        mockMvc.perform(get("/regulatory-rules").param("category", "bebidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("bebidas"));
    }

    @Test
    @WithMockUser
    void list_retorna_lista_vacia_si_no_hay_reglas() throws Exception {
        when(ruleService.findActive(any())).thenReturn(List.of());

        mockMvc.perform(get("/regulatory-rules").param("category", "unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void list_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/regulatory-rules"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /regulatory-rules/{code} ─────────────────────────────────────────

    @Test
    @WithMockUser
    void getByCode_retorna_regla_existente() throws Exception {
        when(ruleService.findByCode("BEB-001")).thenReturn(
                sampleResponse("BEB-001", "bebidas")
        );

        mockMvc.perform(get("/regulatory-rules/{code}", "BEB-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BEB-001"))
                .andExpect(jsonPath("$.category").value("bebidas"))
                .andExpect(jsonPath("$.action").value("DECLARAR_LEYENDA_OBLIGATORIA"))
                .andExpect(jsonPath("$.conditionDsl").exists())
                .andExpect(jsonPath("$.sourceArticle").value("CAA Art. 1"));
    }

    @Test
    @WithMockUser
    void getByCode_retorna_404_si_no_existe() throws Exception {
        when(ruleService.findByCode("XXX-999"))
                .thenThrow(new ResourceNotFoundException("Regla normativa no encontrada: XXX-999"));

        mockMvc.perform(get("/regulatory-rules/{code}", "XXX-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getByCode_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/regulatory-rules/{code}", "BEB-001"))
                .andExpect(status().isUnauthorized());
    }

    // ── Campos del response ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void response_incluye_todos_los_campos_relevantes() throws Exception {
        when(ruleService.findByCode("SNK-001")).thenReturn(
                sampleResponse("SNK-001", "snacks")
        );

        mockMvc.perform(get("/regulatory-rules/{code}", "SNK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.conditionDsl").exists())
                .andExpect(jsonPath("$.action").exists())
                .andExpect(jsonPath("$.actionDetail").exists())
                .andExpect(jsonPath("$.sourceArticle").exists())
                .andExpect(jsonPath("$.validFrom").exists())
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.notes").exists());
    }
}
