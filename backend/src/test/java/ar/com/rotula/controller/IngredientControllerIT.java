package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.dto.IngredientRequest;
import ar.com.rotula.dto.IngredientResponse;
import ar.com.rotula.exception.PercentageSumExceededException;
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
                "Harina de trigo", new BigDecimal("40.000"),
                true, 0, OffsetDateTime.now()
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
                .andExpect(jsonPath("$[0].allergen").value(true))
                .andExpect(jsonPath("$[0].sortOrder").value(0));
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
        IngredientRequest req = new IngredientRequest("Avena", new BigDecimal("30"), null);
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
        IngredientRequest req = new IngredientRequest("", new BigDecimal("10"), null);

        mockMvc.perform(post("/products/{id}/ingredients", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_porcentaje_fuera_de_rango() throws Exception {
        IngredientRequest req = new IngredientRequest("Agua", new BigDecimal("101"), false);

        mockMvc.perform(post("/products/{id}/ingredients", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.percentage").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_422_si_suma_supera_100() throws Exception {
        IngredientRequest req = new IngredientRequest("Agua", new BigDecimal("30"), false);
        when(ingredientService.create(eq(PRODUCT_ID), any()))
                .thenThrow(new PercentageSumExceededException(
                        new BigDecimal("80.000"), new BigDecimal("30.000")));

        mockMvc.perform(post("/products/{id}/ingredients", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── PUT /ingredients/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser
    void update_retorna_ingrediente_modificado() throws Exception {
        IngredientRequest req = new IngredientRequest("Avena integral", new BigDecimal("25"), true);
        IngredientResponse updated = new IngredientResponse(
                INGREDIENT_ID, PRODUCT_ID, TENANT_ID,
                "Avena integral", new BigDecimal("25.000"),
                true, 1, OffsetDateTime.now()
        );
        when(ingredientService.update(eq(INGREDIENT_ID), any())).thenReturn(updated);

        mockMvc.perform(put("/ingredients/{id}", INGREDIENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Avena integral"))
                .andExpect(jsonPath("$.percentage").value(25.0));
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
