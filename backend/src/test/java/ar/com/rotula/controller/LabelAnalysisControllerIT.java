package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.domain.AllergenGroup;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.LabelAnalysisResult;
import ar.com.rotula.service.LabelAnalysisService;
import ar.com.rotula.service.allergen.AllergenDeclarationResult;
import ar.com.rotula.service.nutrition.NutritionCalculationResult;
import ar.com.rotula.service.nutrition.NutritionValues;
import ar.com.rotula.service.nutrition.RoundedNutritionValues;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ar.com.rotula.service.nutrition.SealEvaluatorService.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabelAnalysisController.class)
class LabelAnalysisControllerIT {

    @Autowired MockMvc mockMvc;

    @MockBean LabelAnalysisService analysisService;
    @MockBean JwtService jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // ── Helpers ───────────────────────────────────────────────────────────────

    private NutritionCalculationResult sampleNutrition() {
        RoundedNutritionValues por = new RoundedNutritionValues(110, 460, 2, 24, 9, 0, 0, 0.0, 0);
        RoundedNutritionValues c100 = new RoundedNutritionValues(370, 1550, 7, 80, 29, 1, 0, 0.0, 5);
        NutritionValues raw = new NutritionValues(371.4, 1553.8, 7.14, 80.0, 29.3, 1.07, 0.14, 0.0, 1.71);
        return new NutritionCalculationResult(BigDecimal.valueOf(30), por, c100, raw);
    }

    private AllergenDeclarationResult sampleAllergens(boolean withCross) {
        Set<AllergenGroup> present = EnumSet.of(AllergenGroup.GLUTEN, AllergenGroup.LECHE);
        Set<AllergenGroup> cross = withCross
                ? EnumSet.of(AllergenGroup.MANI)
                : EnumSet.noneOf(AllergenGroup.class);
        String text = withCross
                ? "CONTIENE: Gluten, Leche. PUEDE CONTENER TRAZAS DE: Maní."
                : "CONTIENE: Gluten, Leche.";
        return new AllergenDeclarationResult(present, cross, text);
    }

    // ── GET /products/{id}/analysis ───────────────────────────────────────────

    @Test
    @WithMockUser
    void analyze_retorna_200_con_resultado_completo() throws Exception {
        LabelAnalysisResult result = new LabelAnalysisResult(
                PRODUCT_ID,
                sampleNutrition(),
                Set.of(SEAL_GRASAS_SAT),
                sampleAllergens(true)
        );
        when(analysisService.analyze(PRODUCT_ID)).thenReturn(result);

        mockMvc.perform(get("/products/{id}/analysis", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.nutrition").exists())
                .andExpect(jsonPath("$.nutrition.servingSizeG").value(30))
                .andExpect(jsonPath("$.nutrition.perPortion.energyKcal").value(110))
                .andExpect(jsonPath("$.nutrition.perPortion.proteinsG").value(2))
                .andExpect(jsonPath("$.nutrition.per100g.energyKcal").value(370))
                .andExpect(jsonPath("$.seals").isArray())
                .andExpect(jsonPath("$.allergens.declarationText")
                        .value("CONTIENE: Gluten, Leche. PUEDE CONTENER TRAZAS DE: Maní."));
    }

    @Test
    @WithMockUser
    void analyze_sin_nutricion_retorna_null_en_nutrition_y_seals_vacios() throws Exception {
        LabelAnalysisResult result = new LabelAnalysisResult(
                PRODUCT_ID,
                null,          // sin datos nutricionales
                Set.of(),      // sin sellos
                sampleAllergens(false)
        );
        when(analysisService.analyze(PRODUCT_ID)).thenReturn(result);

        mockMvc.perform(get("/products/{id}/analysis", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutrition").isEmpty())
                .andExpect(jsonPath("$.seals").isArray())
                .andExpect(jsonPath("$.seals").isEmpty());
    }

    @Test
    @WithMockUser
    void analyze_producto_sin_alergenos_declarationText_vacio() throws Exception {
        AllergenDeclarationResult sinAlergenos = new AllergenDeclarationResult(
                EnumSet.noneOf(AllergenGroup.class),
                EnumSet.noneOf(AllergenGroup.class),
                ""
        );
        LabelAnalysisResult result = new LabelAnalysisResult(
                PRODUCT_ID, null, Set.of(), sinAlergenos);
        when(analysisService.analyze(PRODUCT_ID)).thenReturn(result);

        mockMvc.perform(get("/products/{id}/analysis", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergens.declarationText").value(""));
    }

    @Test
    @WithMockUser
    void analyze_multiples_sellos_en_array() throws Exception {
        LabelAnalysisResult result = new LabelAnalysisResult(
                PRODUCT_ID,
                sampleNutrition(),
                Set.of(SEAL_GRASAS_SAT, SEAL_SODIO, SEAL_CALORIAS),
                sampleAllergens(false)
        );
        when(analysisService.analyze(PRODUCT_ID)).thenReturn(result);

        mockMvc.perform(get("/products/{id}/analysis", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seals").isArray())
                .andExpect(jsonPath("$.seals.length()").value(3));
    }

    @Test
    void analyze_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/products/{id}/analysis", PRODUCT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void analyze_producto_no_existente_retorna_404() throws Exception {
        UUID inexistente = UUID.randomUUID();
        when(analysisService.analyze(inexistente))
                .thenThrow(new ar.com.rotula.exception.ResourceNotFoundException(
                        "Producto no encontrado: " + inexistente));

        mockMvc.perform(get("/products/{id}/analysis", inexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    void analyze_incluye_valores_raw_por_100g() throws Exception {
        LabelAnalysisResult result = new LabelAnalysisResult(
                PRODUCT_ID, sampleNutrition(), Set.of(), sampleAllergens(false));
        when(analysisService.analyze(PRODUCT_ID)).thenReturn(result);

        mockMvc.perform(get("/products/{id}/analysis", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutrition.rawPer100g").exists())
                .andExpect(jsonPath("$.nutrition.rawPer100g.energyKcal").isNumber());
    }

    @Test
    @WithMockUser
    void analyze_delegado_al_servicio_con_id_correcto() throws Exception {
        UUID otroId = UUID.randomUUID();
        LabelAnalysisResult result = new LabelAnalysisResult(
                otroId, null, Set.of(), sampleAllergens(false));
        when(analysisService.analyze(otroId)).thenReturn(result);

        mockMvc.perform(get("/products/{id}/analysis", otroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(otroId.toString()));
    }

    @Test
    @WithMockUser
    void analyze_retorna_allergen_groups_como_strings() throws Exception {
        LabelAnalysisResult result = new LabelAnalysisResult(
                PRODUCT_ID, null, Set.of(), sampleAllergens(true));
        when(analysisService.analyze(any())).thenReturn(result);

        mockMvc.perform(get("/products/{id}/analysis", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergens.presentGroups").isArray())
                .andExpect(jsonPath("$.allergens.crossContaminationGroups").isArray());
    }
}
