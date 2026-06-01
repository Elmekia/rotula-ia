package ar.com.rotula.controller;

import ar.com.rotula.config.TenantDataSourceWrapper;
import ar.com.rotula.dto.PageResponse;
import ar.com.rotula.dto.ProductRequest;
import ar.com.rotula.dto.ProductResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.security.JwtService;
import ar.com.rotula.service.ProductService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;
    @MockBean JwtService jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID USER_ID      = UUID.randomUUID();
    private static final UUID PRODUCT_ID   = UUID.randomUUID();
    private static final UUID FOOD_GROUP_ID = UUID.randomUUID();
    private static final UUID FOOD_ITEM_ID  = UUID.randomUUID();

    private ProductResponse sampleResponse() {
        return new ProductResponse(
                PRODUCT_ID, TENANT_ID,
                "Galletitas de avena",          // name
                "Galletitas dulces de avena",   // denomination
                FOOD_GROUP_ID, FOOD_ITEM_ID,    // foodGroupId, foodItemId
                BigDecimal.valueOf(30),          // servingSizeG
                new BigDecimal("200.000"), "g", // netWeight, weightUnit
                null, "12345678",               // rneNumber, rnpaNumber
                Collections.emptyList(),        // crossContaminationGroups
                false,                          // showIngredientPercentages
                // 8 campos nutricionales (todos null)
                null, null, null, null, null, null, null, null,
                "draft", USER_ID, OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    /** ProductRequest mínimo válido para tests del controller. */
    private ProductRequest sampleRequest(String name) {
        return new ProductRequest(
                name, "Denominación de " + name,
                FOOD_GROUP_ID, FOOD_ITEM_ID, null,
                new BigDecimal("200"), "g",
                null, "12345678",
                List.of(), false,
                null, null, null, null, null, null, null, null
        );
    }

    // ── GET /products ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void list_retorna_pagina() throws Exception {
        PageResponse<ProductResponse> page = new PageResponse<>(
                List.of(sampleResponse()), 0, 20, 1, 1, true);
        when(productService.findAll(0, 20, "createdAt,desc")).thenReturn(page);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Galletitas de avena"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_sin_autenticacion_retorna_401() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /products/{id} ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void getById_retorna_producto() throws Exception {
        when(productService.findById(PRODUCT_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.denomination").value("Galletitas dulces de avena"))
                .andExpect(jsonPath("$.rnpaNumber").value("12345678"));
    }

    @Test
    @WithMockUser
    void getById_retorna_404_si_no_existe() throws Exception {
        when(productService.findById(PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado: " + PRODUCT_ID));

        mockMvc.perform(get("/products/{id}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── POST /products ────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void create_retorna_201_con_producto() throws Exception {
        when(productService.create(any(ProductRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest("Galletitas"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_nombre_vacio() throws Exception {
        ProductRequest req = new ProductRequest(
                "", "Denominación válida",
                FOOD_GROUP_ID, FOOD_ITEM_ID, null,
                new BigDecimal("100"), "g",
                null, "12345678",
                List.of(), false,
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_rnpa_vacio() throws Exception {
        ProductRequest req = new ProductRequest(
                "Producto válido", "Denominación válida",
                FOOD_GROUP_ID, FOOD_ITEM_ID, null,
                new BigDecimal("100"), "g",
                null, "",           // rnpaNumber vacío → viola @NotBlank
                List.of(), false,
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.rnpaNumber").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_unidad_invalida() throws Exception {
        ProductRequest req = new ProductRequest(
                "Producto", "Denominación",
                FOOD_GROUP_ID, FOOD_ITEM_ID, null,
                new BigDecimal("100"), "oz",  // unidad inválida
                null, "12345678",
                List.of(), false,
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.weightUnit").exists());
    }

    // ── PUT /products/{id} ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void update_retorna_producto_modificado() throws Exception {
        ProductResponse updated = sampleResponse();
        when(productService.update(eq(PRODUCT_ID), any(ProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/products/{id}", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest("Galletitas Premium"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @WithMockUser
    void update_retorna_404_si_no_existe() throws Exception {
        when(productService.update(eq(PRODUCT_ID), any()))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado: " + PRODUCT_ID));

        mockMvc.perform(put("/products/{id}", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest("Prod"))))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /products/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void delete_retorna_204() throws Exception {
        doNothing().when(productService).delete(PRODUCT_ID);

        mockMvc.perform(delete("/products/{id}", PRODUCT_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(productService).delete(PRODUCT_ID);
    }

    @Test
    @WithMockUser
    void delete_retorna_404_si_no_existe() throws Exception {
        doThrow(new ResourceNotFoundException("Producto no encontrado: " + PRODUCT_ID))
                .when(productService).delete(PRODUCT_ID);

        mockMvc.perform(delete("/products/{id}", PRODUCT_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
