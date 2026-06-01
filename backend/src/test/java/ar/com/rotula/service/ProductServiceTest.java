package ar.com.rotula.service;

import ar.com.rotula.domain.FoodGroup;
import ar.com.rotula.domain.FoodItem;
import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.PageResponse;
import ar.com.rotula.dto.ProductRequest;
import ar.com.rotula.dto.ProductResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.FoodGroupRepository;
import ar.com.rotula.repository.FoodItemRepository;
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

    @Mock ProductRepository   productRepository;
    @Mock FoodGroupRepository foodGroupRepository;
    @Mock FoodItemRepository  foodItemRepository;

    @InjectMocks ProductService productService;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID USER_ID      = UUID.randomUUID();
    private static final UUID PRODUCT_ID   = UUID.randomUUID();
    private static final UUID FOOD_GROUP_ID = UUID.randomUUID();
    private static final UUID FOOD_ITEM_ID  = UUID.randomUUID();

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
                .name("Galletitas de avena")
                .denomination("Galletitas dulces de avena")
                .foodGroupId(FOOD_GROUP_ID)
                .foodItemId(FOOD_ITEM_ID)
                .servingSizeG(BigDecimal.valueOf(30))
                .netWeight(new BigDecimal("200.000"))
                .weightUnit("g")
                .rnpaNumber("12345678")
                .showIngredientPercentages(false)
                .status("draft")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private FoodGroup sampleFoodGroup() {
        return FoodGroup.builder()
                .id(FOOD_GROUP_ID)
                .code("G1")
                .name("Panificados")
                .sortOrder((short) 1)
                .active(true)
                .build();
    }

    private FoodItem sampleFoodItem() {
        return FoodItem.builder()
                .id(FOOD_ITEM_ID)
                .foodGroupId(FOOD_GROUP_ID)
                .name("Galletitas")
                .portionGrams(BigDecimal.valueOf(30))
                .unit("g")
                .sortOrder((short) 1)
                .active(true)
                .build();
    }

    /** ProductRequest completo con los campos mínimos necesarios para los tests. */
    private ProductRequest req(String name, String denomination) {
        return new ProductRequest(
                name, denomination,
                FOOD_GROUP_ID, FOOD_ITEM_ID, null,
                new BigDecimal("200"), "g",
                null, "12345678",
                List.of(), false,
                null, null, null, null, null, null, null, null
        );
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    void findAll_devuelve_pagina_del_tenant() {
        Product p = sampleProduct();
        when(productRepository.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        PageResponse<ProductResponse> result = productService.findAll(0, 20, "name,asc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Galletitas de avena");
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
        assertThat(resp.name()).isEqualTo("Galletitas de avena");
        assertThat(resp.denomination()).isEqualTo("Galletitas dulces de avena");
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
    void create_asigna_tenantId_createdBy_y_porcion_del_alimento() {
        when(foodGroupRepository.findById(FOOD_GROUP_ID)).thenReturn(Optional.of(sampleFoodGroup()));
        when(foodItemRepository.findById(FOOD_ITEM_ID)).thenReturn(Optional.of(sampleFoodItem()));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct());

        productService.create(req("Galletitas de avena", "Galletitas dulces de avena"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product captured = captor.getValue();

        assertThat(captured.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captured.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(captured.getStatus()).isEqualTo("draft");
        // La porción viene del food_item, no del request
        assertThat(captured.getServingSizeG()).isEqualByComparingTo("30");
    }

    @Test
    void create_lanza_excepcion_si_food_item_no_pertenece_al_grupo() {
        UUID otroGrupo = UUID.randomUUID();
        FoodItem itemDeOtroGrupo = FoodItem.builder()
                .id(FOOD_ITEM_ID)
                .foodGroupId(otroGrupo)   // pertenece a otro grupo
                .name("Otro alimento")
                .portionGrams(BigDecimal.valueOf(50))
                .unit("g")
                .active(true)
                .build();
        when(foodGroupRepository.findById(FOOD_GROUP_ID)).thenReturn(Optional.of(sampleFoodGroup()));
        when(foodItemRepository.findById(FOOD_ITEM_ID)).thenReturn(Optional.of(itemDeOtroGrupo));

        assertThatThrownBy(() -> productService.create(req("X", "Y")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_modifica_campos_y_porcion() {
        Product existing = sampleProduct();
        FoodItem nuevoItem = FoodItem.builder()
                .id(FOOD_ITEM_ID).foodGroupId(FOOD_GROUP_ID)
                .name("Galletitas premium").portionGrams(BigDecimal.valueOf(40))
                .unit("g").active(true).build();

        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));
        when(foodGroupRepository.findById(FOOD_GROUP_ID)).thenReturn(Optional.of(sampleFoodGroup()));
        when(foodItemRepository.findById(FOOD_ITEM_ID)).thenReturn(Optional.of(nuevoItem));
        when(productRepository.save(any(Product.class))).thenReturn(existing);

        productService.update(PRODUCT_ID, req("Galletitas premium", "Galletitas dulces premium"));

        assertThat(existing.getName()).isEqualTo("Galletitas premium");
        assertThat(existing.getDenomination()).isEqualTo("Galletitas dulces premium");
        // La porción se actualiza al valor del nuevo food_item
        assertThat(existing.getServingSizeG()).isEqualByComparingTo("40");
        verify(productRepository).save(existing);
    }

    @Test
    void update_lanza_excepcion_si_producto_no_es_del_tenant() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(PRODUCT_ID, req("Otro", "Cat")))
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
