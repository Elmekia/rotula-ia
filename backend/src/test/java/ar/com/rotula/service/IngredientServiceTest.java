package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.IngredientRequest;
import ar.com.rotula.dto.IngredientResponse;
import ar.com.rotula.exception.PercentageSumExceededException;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class IngredientServiceTest {

    @Mock IngredientRepository ingredientRepository;
    @Mock ProductRepository    productRepository;
    @InjectMocks IngredientService ingredientService;

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID USER_ID       = UUID.randomUUID();
    private static final UUID PRODUCT_ID    = UUID.randomUUID();
    private static final UUID INGREDIENT_ID = UUID.randomUUID();
    private static final UUID NO_EXCLUDE    = new UUID(0, 0);

    @BeforeEach
    void setupSecurityContext() {
        AppUserDetails principal = new AppUserDetails(USER_ID, TENANT_ID, "test@test.com", "hash", "admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Ingredient sampleIngredient() {
        return Ingredient.builder()
                .id(INGREDIENT_ID)
                .productId(PRODUCT_ID)
                .tenantId(TENANT_ID)
                .name("Harina de trigo")
                .percentage(new BigDecimal("40.000"))
                .allergen(true)
                .sortOrder(0)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Product sampleProduct() {
        return Product.builder()
                .id(PRODUCT_ID)
                .tenantId(TENANT_ID)
                .name("Galletitas")
                .category("Panificados")
                .netWeight(new BigDecimal("200"))
                .weightUnit("g")
                .status("draft")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // ── findByProduct ────────────────────────────────────────────────────────

    @Test
    void findByProduct_retorna_lista_ordenada_por_porcentaje() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByPercentageDesc(PRODUCT_ID, TENANT_ID))
                .thenReturn(List.of(sampleIngredient()));

        List<IngredientResponse> result = ingredientService.findByProduct(PRODUCT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Harina de trigo");
        assertThat(result.get(0).allergen()).isTrue();
    }

    @Test
    void findByProduct_lanza_excepcion_si_producto_no_es_del_tenant() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.findByProduct(PRODUCT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_asigna_tenantId_y_productId() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.sumPercentageExcluding(eq(PRODUCT_ID), eq(TENANT_ID), any()))
                .thenReturn(BigDecimal.ZERO);
        when(ingredientRepository.save(any())).thenReturn(sampleIngredient());

        IngredientRequest req = new IngredientRequest("Harina de trigo", new BigDecimal("40"), null);
        ingredientService.create(PRODUCT_ID, req);

        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepository).save(captor.capture());
        Ingredient saved = captor.getValue();

        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void create_autodetecta_alergeno_cuando_allergen_es_null() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.sumPercentageExcluding(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(ingredientRepository.save(any())).thenReturn(sampleIngredient());

        // "avena" es alérgeno según Res. 109/2023
        IngredientRequest req = new IngredientRequest("Avena molida", new BigDecimal("20"), null);
        ingredientService.create(PRODUCT_ID, req);

        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepository).save(captor.capture());
        assertThat(captor.getValue().isAllergen()).isTrue();
    }

    @Test
    void create_respeta_allergen_explicito_false() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.sumPercentageExcluding(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        Ingredient saved = sampleIngredient();
        saved.setAllergen(false);
        when(ingredientRepository.save(any())).thenReturn(saved);

        // aunque "trigo" es alérgeno, si el cliente envía allergen=false se respeta
        IngredientRequest req = new IngredientRequest("Harina de trigo", new BigDecimal("40"), false);
        ingredientService.create(PRODUCT_ID, req);

        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepository).save(captor.capture());
        assertThat(captor.getValue().isAllergen()).isFalse();
    }

    @Test
    void create_lanza_excepcion_si_suma_supera_100() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        // Suma existente = 80%, nuevo ingrediente = 30% → total 110% > 100%
        when(ingredientRepository.sumPercentageExcluding(any(), any(), any()))
                .thenReturn(new BigDecimal("80.000"));

        IngredientRequest req = new IngredientRequest("Agua", new BigDecimal("30"), false);
        assertThatThrownBy(() -> ingredientService.create(PRODUCT_ID, req))
                .isInstanceOf(PercentageSumExceededException.class)
                .hasMessageContaining("100");
    }

    @Test
    void create_acepta_suma_exactamente_100() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.sumPercentageExcluding(any(), any(), any()))
                .thenReturn(new BigDecimal("60.000"));
        when(ingredientRepository.save(any())).thenReturn(sampleIngredient());

        // 60 + 40 = 100 → válido
        IngredientRequest req = new IngredientRequest("Azúcar", new BigDecimal("40"), false);
        assertThatNoException().isThrownBy(() -> ingredientService.create(PRODUCT_ID, req));
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_modifica_campos_y_revalida_suma() {
        Ingredient existing = sampleIngredient();
        when(ingredientRepository.findByIdAndTenantId(INGREDIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));
        // Suma excluyendo este ingrediente = 50%, nuevo = 30% → total 80% ≤ 100% ✓
        when(ingredientRepository.sumPercentageExcluding(PRODUCT_ID, TENANT_ID, INGREDIENT_ID))
                .thenReturn(new BigDecimal("50.000"));
        when(ingredientRepository.save(any())).thenReturn(existing);

        IngredientRequest req = new IngredientRequest("Harina integral", new BigDecimal("30"), null);
        ingredientService.update(INGREDIENT_ID, req);

        assertThat(existing.getName()).isEqualTo("Harina integral");
        assertThat(existing.getPercentage()).isEqualByComparingTo("30");
    }

    @Test
    void update_lanza_excepcion_si_no_pertenece_al_tenant() {
        when(ingredientRepository.findByIdAndTenantId(INGREDIENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        IngredientRequest req = new IngredientRequest("X", new BigDecimal("10"), false);
        assertThatThrownBy(() -> ingredientService.update(INGREDIENT_ID, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_elimina_ingrediente_del_tenant() {
        Ingredient existing = sampleIngredient();
        when(ingredientRepository.findByIdAndTenantId(INGREDIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));

        ingredientService.delete(INGREDIENT_ID);

        verify(ingredientRepository).delete(existing);
    }

    @Test
    void delete_lanza_excepcion_si_no_existe() {
        when(ingredientRepository.findByIdAndTenantId(INGREDIENT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.delete(INGREDIENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ingredientRepository, never()).delete(any());
    }

    // ── AllergenDetector ─────────────────────────────────────────────────────

    @Test
    void allergenDetector_detecta_grupos_principales() {
        assertThat(AllergenDetector.isAllergen("Harina de trigo")).isTrue();         // 1 gluten
        assertThat(AllergenDetector.isAllergen("Leche descremada")).isTrue();        // 7 leche
        assertThat(AllergenDetector.isAllergen("Huevo entero")).isTrue();            // 3 huevo
        assertThat(AllergenDetector.isAllergen("Maní tostado")).isTrue();            // 5 maní
        assertThat(AllergenDetector.isAllergen("Lecitina de soja")).isTrue();        // 6 soja
        assertThat(AllergenDetector.isAllergen("Almendra fileteada")).isTrue();      // 8 frutos cáscara
        assertThat(AllergenDetector.isAllergen("Sésamo negro")).isTrue();            // 11 sésamo
        assertThat(AllergenDetector.isAllergen("Mostaza amarilla")).isTrue();        // 10 mostaza
    }

    @Test
    void allergenDetector_detecta_keywords_nuevos() {
        assertThat(AllergenDetector.isAllergen("Extracto de malta")).isTrue();       // 1 gluten
        assertThat(AllergenDetector.isAllergen("Caseinato de sodio")).isTrue();      // 7 leche
        assertThat(AllergenDetector.isAllergen("Lactosuero en polvo")).isTrue();     // 7 leche
        assertThat(AllergenDetector.isAllergen("Corvina del Plata")).isTrue();       // 4 pescado
        assertThat(AllergenDetector.isAllergen("Miso de soja")).isTrue();            // 6 soja
        assertThat(AllergenDetector.isAllergen("Tempeh orgánico")).isTrue();         // 6 soja
        assertThat(AllergenDetector.isAllergen("Sepia en tinta")).isTrue();          // 14 molusco
    }

    @Test
    void allergenDetector_no_genera_falsos_positivos() {
        assertThat(AllergenDetector.isAllergen("Agua")).isFalse();
        assertThat(AllergenDetector.isAllergen("Sal fina")).isFalse();
        assertThat(AllergenDetector.isAllergen("Aceite de girasol")).isFalse();
        assertThat(AllergenDetector.isAllergen("Azúcar")).isFalse();
        assertThat(AllergenDetector.isAllergen("Colorante caramelo")).isFalse();
        assertThat(AllergenDetector.isAllergen("Nuez moscada")).isFalse();           // especia, no alérgeno
    }

    @Test
    void allergenDetector_es_case_insensitive() {
        assertThat(AllergenDetector.isAllergen("TRIGO")).isTrue();
        assertThat(AllergenDetector.isAllergen("Leche ENTERA")).isTrue();
        assertThat(AllergenDetector.isAllergen("MALTA")).isTrue();
    }
}
