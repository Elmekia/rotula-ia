package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.IngredientRequest;
import ar.com.rotula.dto.IngredientResponse;
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
                .weightGrams(new BigDecimal("200.000"))
                .allergen(true)
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
    void findByProduct_retorna_lista_ordenada_con_porcentaje_calculado() {
        Ingredient ing = sampleIngredient();
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(PRODUCT_ID, TENANT_ID))
                .thenReturn(List.of(ing));

        List<IngredientResponse> result = ingredientService.findByProduct(PRODUCT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Harina de trigo");
        assertThat(result.get(0).weightGrams()).isEqualByComparingTo("200.000");
        // Ingrediente único → 200/200 * 100 = 100%
        assertThat(result.get(0).percentage()).isEqualByComparingTo("100.000");
        assertThat(result.get(0).allergen()).isTrue();
    }

    @Test
    void findByProduct_calcula_porcentaje_proporcional_con_varios_ingredientes() {
        Ingredient ing1 = Ingredient.builder()
                .id(UUID.randomUUID()).productId(PRODUCT_ID).tenantId(TENANT_ID)
                .name("Harina de trigo").weightGrams(new BigDecimal("300.000"))
                .allergen(true).createdAt(OffsetDateTime.now()).build();
        Ingredient ing2 = Ingredient.builder()
                .id(UUID.randomUUID()).productId(PRODUCT_ID).tenantId(TENANT_ID)
                .name("Azúcar").weightGrams(new BigDecimal("100.000"))
                .allergen(false).createdAt(OffsetDateTime.now()).build();

        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(PRODUCT_ID, TENANT_ID))
                .thenReturn(List.of(ing1, ing2));   // ya ordenados por weight DESC

        List<IngredientResponse> result = ingredientService.findByProduct(PRODUCT_ID);

        // Total = 400g → ing1 = 75%, ing2 = 25%
        assertThat(result).hasSize(2);
        assertThat(result.get(0).percentage()).isEqualByComparingTo("75.000");
        assertThat(result.get(1).percentage()).isEqualByComparingTo("25.000");
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
        when(ingredientRepository.save(any())).thenReturn(sampleIngredient());
        when(ingredientRepository.sumWeightGrams(PRODUCT_ID, TENANT_ID))
                .thenReturn(new BigDecimal("200.000"));

        IngredientRequest req = new IngredientRequest("Harina de trigo", new BigDecimal("200"), null);
        ingredientService.create(PRODUCT_ID, req);

        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepository).save(captor.capture());
        Ingredient saved = captor.getValue();

        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(saved.getWeightGrams()).isEqualByComparingTo("200");
    }

    @Test
    void create_autodetecta_alergeno_cuando_allergen_es_null() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        when(ingredientRepository.save(any())).thenReturn(sampleIngredient());
        when(ingredientRepository.sumWeightGrams(PRODUCT_ID, TENANT_ID))
                .thenReturn(new BigDecimal("100.000"));

        // "avena" es alérgeno según Res. 109/2023
        IngredientRequest req = new IngredientRequest("Avena molida", new BigDecimal("100"), null);
        ingredientService.create(PRODUCT_ID, req);

        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepository).save(captor.capture());
        assertThat(captor.getValue().isAllergen()).isTrue();
    }

    @Test
    void create_respeta_allergen_explicito_false() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(sampleProduct()));
        Ingredient saved = sampleIngredient();
        saved.setAllergen(false);
        when(ingredientRepository.save(any())).thenReturn(saved);
        when(ingredientRepository.sumWeightGrams(PRODUCT_ID, TENANT_ID))
                .thenReturn(new BigDecimal("200.000"));

        // aunque "trigo" es alérgeno, si el cliente envía allergen=false se respeta
        IngredientRequest req = new IngredientRequest("Harina de trigo", new BigDecimal("200"), false);
        ingredientService.create(PRODUCT_ID, req);

        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepository).save(captor.capture());
        assertThat(captor.getValue().isAllergen()).isFalse();
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_modifica_campos() {
        Ingredient existing = sampleIngredient();
        when(ingredientRepository.findByIdAndTenantId(INGREDIENT_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));
        when(ingredientRepository.save(any())).thenReturn(existing);
        when(ingredientRepository.sumWeightGrams(PRODUCT_ID, TENANT_ID))
                .thenReturn(new BigDecimal("150.000"));

        IngredientRequest req = new IngredientRequest("Harina integral", new BigDecimal("150"), null);
        ingredientService.update(INGREDIENT_ID, req);

        assertThat(existing.getName()).isEqualTo("Harina integral");
        assertThat(existing.getWeightGrams()).isEqualByComparingTo("150");
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
    void allergenDetector_detecta_ingredientes_comunes() {
        assertThat(AllergenDetector.isAllergen("Harina de trigo")).isTrue();
        assertThat(AllergenDetector.isAllergen("Leche descremada")).isTrue();
        assertThat(AllergenDetector.isAllergen("Huevo entero")).isTrue();
        assertThat(AllergenDetector.isAllergen("Maní tostado")).isTrue();
        assertThat(AllergenDetector.isAllergen("Lecitina de soja")).isTrue();
        assertThat(AllergenDetector.isAllergen("Almendra fileteada")).isTrue();
        assertThat(AllergenDetector.isAllergen("Sésamo negro")).isTrue();
        assertThat(AllergenDetector.isAllergen("Mostaza amarilla")).isTrue();
    }

    @Test
    void allergenDetector_no_genera_falsos_positivos() {
        assertThat(AllergenDetector.isAllergen("Agua")).isFalse();
        assertThat(AllergenDetector.isAllergen("Sal fina")).isFalse();
        assertThat(AllergenDetector.isAllergen("Aceite de girasol")).isFalse();
        assertThat(AllergenDetector.isAllergen("Azúcar")).isFalse();
        assertThat(AllergenDetector.isAllergen("Colorante caramelo")).isFalse();
        assertThat(AllergenDetector.isAllergen("Nuez moscada")).isFalse();
    }

    @Test
    void allergenDetector_es_case_insensitive() {
        assertThat(AllergenDetector.isAllergen("TRIGO")).isTrue();
        assertThat(AllergenDetector.isAllergen("Leche ENTERA")).isTrue();
    }
}
