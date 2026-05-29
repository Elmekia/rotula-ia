package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.dto.label.LabelDTO;
import ar.com.rotula.dto.label.LabelVersionResponse;
import ar.com.rotula.dto.label.LabelVersionSummary;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.LabelGenerationService;
import ar.com.rotula.service.allergen.AllergenDeclarationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabelController.class)
class LabelControllerIT {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean LabelGenerationService labelGenerationService;
    @MockBean JwtService             jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID LABEL_ID   = UUID.fromString("aaaabbbb-cccc-dddd-eeee-ffffffffffff");

    // ── POST /labels/generate ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void generate_retorna_201_con_label_dto() throws Exception {
        when(labelGenerationService.generate(PRODUCT_ID)).thenReturn(sampleResponse(1));

        mockMvc.perform(post("/labels/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                            put("productId", PRODUCT_ID.toString());
                        }})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.labelData").exists())
                .andExpect(jsonPath("$.labelData.productName").value("Producto Test"));
    }

    @Test
    @WithMockUser
    void generate_segunda_version_retorna_version_2() throws Exception {
        when(labelGenerationService.generate(PRODUCT_ID)).thenReturn(sampleResponse(2));

        mockMvc.perform(post("/labels/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void generate_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(post("/labels/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT_ID + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void generate_product_not_found_retorna_404() throws Exception {
        when(labelGenerationService.generate(any()))
                .thenThrow(new ar.com.rotula.exception.ResourceNotFoundException("Producto no encontrado"));

        mockMvc.perform(post("/labels/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + PRODUCT_ID + "\"}"))
                .andExpect(status().isNotFound());
    }

    // ── GET /labels/{productId}/versions ─────────────────────────────────────

    @Test
    @WithMockUser
    void getVersions_retorna_lista_de_versiones() throws Exception {
        List<LabelVersionSummary> summaries = List.of(
                new LabelVersionSummary(LABEL_ID, PRODUCT_ID, 2, "draft", "Galletita", OffsetDateTime.now()),
                new LabelVersionSummary(UUID.randomUUID(), PRODUCT_ID, 1, "draft", "Galletita", OffsetDateTime.now())
        );
        when(labelGenerationService.getVersions(PRODUCT_ID)).thenReturn(summaries);

        mockMvc.perform(get("/labels/{productId}/versions", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[1].version").value(1));
    }

    @Test
    @WithMockUser
    void getVersions_sin_versiones_retorna_lista_vacia() throws Exception {
        when(labelGenerationService.getVersions(PRODUCT_ID)).thenReturn(List.of());

        mockMvc.perform(get("/labels/{productId}/versions", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getVersions_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/labels/{productId}/versions", PRODUCT_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LabelVersionResponse sampleResponse(int version) {
        AllergenDeclarationResult allergens = new AllergenDeclarationResult(
                EnumSet.noneOf(ar.com.rotula.domain.AllergenGroup.class),
                EnumSet.noneOf(ar.com.rotula.domain.AllergenGroup.class), "");

        LabelDTO dto = new LabelDTO(
                PRODUCT_ID, "Producto Test", "Galletita",
                "panificados", BigDecimal.valueOf(200), "g",
                null, null,
                List.of(),
                null,
                Set.of(),
                allergens,
                List.of(),
                false
        );

        return new LabelVersionResponse(LABEL_ID, PRODUCT_ID, version, "draft", dto, OffsetDateTime.now());
    }
}
