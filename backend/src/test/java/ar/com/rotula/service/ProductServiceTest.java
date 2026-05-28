package ar.com.rotula.service;

import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.PageResponse;
import ar.com.rotula.dto.ProductRequest;
import ar.com.rotula.dto.ProductResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductService productService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeEach
    void setupSecurityContext() {
        AppUserDetails principal = new AppUserDetails(
                USER_ID, TENANT_ID, "test@tenant.com", "hash", "admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Product sampleProduct() {
        return Product.builder()
                .id(PRODUCT_ID)
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .name("Yerba Mate")
                .category("Infusiones")
                .netWeight(new BigDecimal("500.000"))
                .weightUnit("g")
                .rneNumber("RNE-001")
                .rnpaNumber("RNPA-001")
                .status("draft")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    /** Helper para crear ProductRequest sin campos opcionales nuevos. */
    private static ProductRequest req(String name, String category, BigDecimal weight,
                                      String unit, String rne, String rnpa) {
        return new ProductRequest(name, category, weight, unit, rne, rnpa, null, null);
    }

    // ── findAll ─────────────────────────────────────────────────────────────

    @Test
    void findAll_devuelve_pagina_del_tenant() {
        Product p = sampleProduct();
        when(productRepository.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        PageResponse<ProductResponse> result = productService.findAll(0, 20, "name,asc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Yerba Mate");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
    }

    @Test
    void findAll_pagina_vacia_cuando_no_hay_productos() {
        when(productRepository.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<ProductResponse> result = productService.findAll(0, 20, null);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_retorna_producto_del_tenant() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));

        ProductResponse resp = productService.findById(PRODUCT_ID);

        assertThat(resp.id()).isEqualTo(PRODUCT_ID);
        assertThat(resp.name()).isEqualTo("Yerba Mate");
    }

    @Test
    void findById_lanza_excepcion_si_no_existe() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(PRODUCT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(PRODUCT_ID.toString());
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_asigna_tenantId_y_createdBy() {
        Product saved = sampleProduct();
        saved.setName("Galletitas");
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        productService.create(req("Galletitas", "Panificados", new BigDecimal("150"), "g", null, null));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product captured = captor.getValue();

        assertThat(captured.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captured.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(captured.getStatus()).isEqualTo("draft");
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_modifica_campos_del_producto() {
        Product existing = sampleProduct();
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        productService.update(PRODUCT_ID,
                req("Yerba Mate Premium", "Infusiones", new BigDecimal("1000"), "g", "RNE-002", "RNPA-002"));

        assertThat(existing.getName()).isEqualTo("Yerba Mate Premium");
        assertThat(existing.getNetWeight()).isEqualByComparingTo("1000");
        assertThat(existing.getRneNumber()).isEqualTo("RNE-002");
        verify(productRepository).save(existing);
    }

    @Test
    void update_lanza_excepcion_si_producto_no_es_del_tenant() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(PRODUCT_ID,
                req("Otro", "Cat", new BigDecimal("100"), "g", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_elimina_producto_del_tenant() {
        Product existing = sampleProduct();
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));

        productService.delete(PRODUCT_ID);

        verify(productRepository).delete(existing);
    }

    @Test
    void delete_lanza_excepcion_si_no_existe() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(PRODUCT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any());
    }
}
