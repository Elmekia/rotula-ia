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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test for ProductController.
 *
 * - @MockBean JwtService: lets the real JwtAuthFilter load (it depends on JwtService) but
 *   requests without a Bearer token pass through automatically.
 * - @WithMockUser: sets up an authenticated principal in the SecurityContext so that
 *   Spring Security's AuthorizationFilter allows the request.
 * - No CSRF tokens needed because SecurityConfig explicitly disables CSRF.
 */
@WebMvcTest(ProductController.class)
class ProductControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;
    @MockBean JwtService jwtService;
    @MockBean TenantDataSourceWrapper tenantDataSourceWrapper;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private ProductResponse sampleResponse() {
        return new ProductResponse(
                PRODUCT_ID, TENANT_ID, "Yerba Mate", "Infusiones",
                new BigDecimal("500.000"), "g", "RNE-001", "RNPA-001",
                "draft",
                null, null,   // servingSizeG, crossContamination
                USER_ID, OffsetDateTime.now(), OffsetDateTime.now()
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
                .andExpect(jsonPath("$.content[0].name").value("Yerba Mate"))
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
                .andExpect(jsonPath("$.category").value("Infusiones"));
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
        ProductRequest req = new ProductRequest(
                "Galletitas", "Panificados", new BigDecimal("150"), "g",
                null, null, null, null);
        when(productService.create(any(ProductRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_nombre_vacio() throws Exception {
        ProductRequest req = new ProductRequest(
                "", "Cat", new BigDecimal("100"), "g", null, null, null, null);

        mockMvc.perform(post("/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    @WithMockUser
    void create_retorna_400_si_unidad_invalida() throws Exception {
        ProductRequest req = new ProductRequest(
                "Producto", "Cat", new BigDecimal("100"), "oz", null, null, null, null);

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
        ProductRequest req = new ProductRequest(
                "Yerba Premium", "Infusiones", new BigDecimal("1000"), "g",
                "RNE-002", null, null, null);
        ProductResponse updated = new ProductResponse(
                PRODUCT_ID, TENANT_ID, "Yerba Premium", "Infusiones",
                new BigDecimal("1000"), "g", "RNE-002", null,
                "draft",
                null, null,   // servingSizeG, crossContamination
                USER_ID, OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(productService.update(eq(PRODUCT_ID), any(ProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/products/{id}", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yerba Premium"));
    }

    @Test
    @WithMockUser
    void update_retorna_404_si_no_existe() throws Exception {
        ProductRequest req = new ProductRequest(
                "Prod", "Cat", new BigDecimal("100"), "g", null, null, null, null);
        when(productService.update(eq(PRODUCT_ID), any()))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado: " + PRODUCT_ID));

        mockMvc.perform(put("/products/{id}", PRODUCT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /products/{id} ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void delete_retorna_204() throws Exception {
        doNothing().when(productService).delete(PRODUCT_ID);

        mockMvc.perform(delete("/products/{id}", PRODUCT_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(productService).delete(PRODUCT_ID);
    }

    @Test
    @WithMockUser
    void delete_retorna_404_si_no_existe() throws Exception {
        doThrow(new ResourceNotFoundException("Producto no encontrado: " + PRODUCT_ID))
                .when(productService).delete(PRODUCT_ID);

        mockMvc.perform(delete("/products/{id}", PRODUCT_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
