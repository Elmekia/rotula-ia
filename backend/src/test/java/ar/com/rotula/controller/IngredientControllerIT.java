package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.dto.IngredientRequest;
import ar.com.rotula.dto.IngredientResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.IngredientService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IngredientController.class)
class IngredientControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean IngredientService ingredientService;
    @MockBean JwtService jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID PRODUCT_ID    = UUID.randomUUID();
    private static final UUID INGREDIENT_ID = UUID.randomUUID();

    private IngredientResponse sampleResponse() {
        return new IngredientResponse(
                INGREDIENT_ID, PRODUCT_ID, TENANT_ID,
                "Harina de trigo",
                new BigDecimal("200.000"),   // weightGrams
                new BigDecimal("100.000"),   // percentage (calculado)
                true,
                OffsetDateTime.now()
        );
    }

    // ── GET /products/{id}/ingredients ───────────────────────────────────────

    @Test
    @WithMockUser
    void list_retorna_ingredientes() throws Exception {
        when(ingredientService.findByProduct(PRODUCT_ID)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/products/{id}/ingredients", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Harina de trigo"))
                .andExpect(jsonPath("$[0].weightGrams").value(200.0))
                .andExpect(jsonPath("$[0].percentage").value(100.0))
                .andExpect(jsonPath("$[0].allergen").value(true));
    }

    @Test
    @WithMockUser
    void list_retorna_404_si_producto_no_existe() throws Exception {
        when(ingredientService.findByProduct(PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado: " + PRODUCT_ID));

        mockMvc.perform(get("/products/{id}/ingredients", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void list_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/products/{id}/ingredients", PRODUCT_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /products/{id}/ingredients ──────────────────────────────────────

    @Test
    @WithMockUser
    void create_retorna_201() throws Exception {
        IngredientRequest req = new IngredientRequest("Avena", new BigDecimal("150"), null);
        when(ingredientService.create(eq(PRODUCT_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/products/{id}/ingredients", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_nombre_vacio() throws Exception {
        IngredientRequest req = new IngredientRequest("", new BigDecimal("100"), null);

        mockMvc.perform(post("/products/{id}/ingredients", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_peso_invalido() throws Exception {
        // weight_grams = 0 viola @DecimalMin("0.001")
        IngredientRequest req = new IngredientRequest("Agua", BigDecimal.ZERO, false);

        mockMvc.perform(post("/products/{id}/ingredients", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.weightGrams").exists());
    }

    // ── PUT /ingredients/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser
    void update_retorna_ingrediente_modificado() throws Exception {
        IngredientRequest req = new IngredientRequest("Avena integral", new BigDecimal("250"), true);
        IngredientResponse updated = new IngredientResponse(
                INGREDIENT_ID, PRODUCT_ID, TENANT_ID,
                "Avena integral",
                new BigDecimal("250.000"),
                new BigDecimal("100.000"),
                true,
                OffsetDateTime.now()
        );
        when(ingredientService.update(eq(INGREDIENT_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/ingredients/{id}", INGREDIENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Avena integral"))
                .andExpect(jsonPath("$.weightGrams").value(250.0))
                .andExpect(jsonPath("$.percentage").value(100.0));
    }

    @Test
    @WithMockUser
    void update_retorna_404_si_no_existe() throws Exception {
        IngredientRequest req = new IngredientRequest("X", new BigDecimal("10"), false);
        when(ingredientService.update(eq(INGREDIENT_ID), any()))
                .thenThrow(new ResourceNotFoundException("Ingrediente no encontrado: " + INGREDIENT_ID));

        mockMvc.perform(put("/ingredients/{id}", INGREDIENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /ingredients/{id} ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void delete_retorna_204() throws Exception {
        doNothing().when(ingredientService).delete(INGREDIENT_ID);

        mockMvc.perform(delete("/ingredients/{id}", INGREDIENT_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(ingredientService).delete(INGREDIENT_ID);
    }

    @Test
    @WithMockUser
    void delete_retorna_404_si_no_existe() throws Exception {
        doThrow(new ResourceNotFoundException("Ingrediente no encontrado: " + INGREDIENT_ID))
                .when(ingredientService).delete(INGREDIENT_ID);

        mockMvc.perform(delete("/ingredients/{id}", INGREDIENT_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
