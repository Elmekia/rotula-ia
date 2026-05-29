package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.domain.Product;
import ar.com.rotula.domain.RegulatoryName;
import ar.com.rotula.dto.LegalNameSuggestion;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.repository.RegulatoryNameRepository;
import ar.com.rotula.service.nutrition.NutritionCalculationResult;
import ar.com.rotula.service.nutrition.NutritionCalculatorService;
import ar.com.rotula.service.nutrition.NutritionValues;
import ar.com.rotula.service.nutrition.RoundedNutritionValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalNameServiceTest {

    @Mock ProductRepository          productRepository;
    @Mock IngredientRepository       ingredientRepository;
    @Mock RegulatoryNameRepository   regulatoryNameRepository;
    @Mock NutritionCalculatorService nutritionCalculator;

    @InjectMocks LegalNameService service;

    private static final UUID TENANT_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PRODUCT_ID  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private Product product;
    private RegulatoryName regulatoryName;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(PRODUCT_ID)
                .tenantId(TENANT_ID)
                .name("Leche La Vaca Feliz")
                .category("leche fluida")
                .netWeight(BigDecimal.valueOf(1000))
                .weightUnit("ml")
                .status("active")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        regulatoryName = RegulatoryName.builder()
                .id(UUID.randomUUID())
                .category("leche fluida")
                .baseName("Leche entera pasteurizada")
                .sourceArticle("CAA Art. 559 § 1")
                .active(true)
                .build();
    }

    // ── Lookup por categoría ──────────────────────────────────────────────────

    @Test
    void suggest_categoria_encontrada_retorna_nombre_legal() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(product));
        when(regulatoryNameRepository.findByCategoryIgnoreCaseAndActiveTrue("leche fluida"))
                .thenReturn(Optional.of(regulatoryName));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of());

        LegalNameSuggestion result = service.suggest(PRODUCT_ID, TENANT_ID);

        assertThat(result.suggestedName()).isEqualTo("Leche entera pasteurizada");
        assertThat(result.sourceArticle()).isEqualTo("CAA Art. 559 § 1");
    }

    @Test
    void suggest_categoria_no_encontrada_retorna_null_sin_alerta() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(product));
        when(regulatoryNameRepository.findByCategoryIgnoreCaseAndActiveTrue(anyString()))
                .thenReturn(Optional.empty());

        LegalNameSuggestion result = service.suggest(PRODUCT_ID, TENANT_ID);

        assertThat(result.suggestedName()).isNull();
        assertThat(result.modifiers()).isEmpty();
        assertThat(result.alertDiffers()).isFalse();
        assertThat(result.sourceArticle()).isNull();
    }

    @Test
    void suggest_producto_no_existente_lanza_exception() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suggest(PRODUCT_ID, TENANT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Alerta por denominación diferente ─────────────────────────────────────

    @Test
    void suggest_nombre_producto_difiere_de_legal_alerta_true() {
        // "Lactito Bebible" no contiene palabras de "Leche entera pasteurizada"
        product.setName("Lactito Bebible");
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(product));
        when(regulatoryNameRepository.findByCategoryIgnoreCaseAndActiveTrue(anyString()))
                .thenReturn(Optional.of(regulatoryName));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of());

        LegalNameSuggestion result = service.suggest(PRODUCT_ID, TENANT_ID);

        assertThat(result.alertDiffers()).isTrue();
    }

    @Test
    void suggest_nombre_producto_contiene_nombre_legal_alerta_false() {
        // "Leche La Vaca Feliz" contiene "leche" que está en la denominación legal
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(product));
        when(regulatoryNameRepository.findByCategoryIgnoreCaseAndActiveTrue(anyString()))
                .thenReturn(Optional.of(regulatoryName));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of());

        LegalNameSuggestion result = service.suggest(PRODUCT_ID, TENANT_ID);

        assertThat(result.alertDiffers()).isFalse();
    }

    // ── productNameMatchesLegal (unit tests de la función helper) ─────────────

    @Test
    void productNameMatchesLegal_coincidencia_parcial_retorna_true() {
        assertThat(service.productNameMatchesLegal("Leche La Serenísima", "Leche entera pasteurizada"))
                .isTrue();
    }

    @Test
    void productNameMatchesLegal_sin_coincidencia_retorna_false() {
        assertThat(service.productNameMatchesLegal("Bebida Tropical", "Leche entera pasteurizada"))
                .isFalse();
    }

    @Test
    void productNameMatchesLegal_ignorar_palabras_cortas() {
        // "de" tiene 2 letras, se ignora; "Sal" tiene 3, se ignora; "mesa" tiene 4 → match
        assertThat(service.productNameMatchesLegal("Sal mesa fina", "Sal de mesa"))
                .isTrue();
    }

    @Test
    void productNameMatchesLegal_sin_acento_igual() {
        // "yogur" sin tilde == "Yogur"
        assertThat(service.productNameMatchesLegal("Yogur natural batido", "Yogur"))
                .isTrue();
    }

    // ── Modificadores nutricionales ───────────────────────────────────────────

    @Test
    void buildNutritionalModifiers_sin_porcion_retorna_lista_vacia() {
        // product sin servingSizeG → sin modificadores
        product.setServingSizeG(null);
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of(ingredientWithWeight(200)));

        List<String> mods = service.buildNutritionalModifiers(PRODUCT_ID, TENANT_ID, product);
        assertThat(mods).isEmpty();
    }

    @Test
    void buildNutritionalModifiers_bajo_en_sodio_cuando_sodium_lt_120() {
        product.setServingSizeG(BigDecimal.valueOf(30));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of(ingredientWithWeight(100)));
        when(nutritionCalculator.calculate(any(), any(), any()))
                .thenReturn(nutritionResult(0, 0, 0, 0, 0, 0, 0, 50)); // sodium 50 mg

        List<String> mods = service.buildNutritionalModifiers(PRODUCT_ID, TENANT_ID, product);
        assertThat(mods).contains("Bajo en sodio");
    }

    @Test
    void buildNutritionalModifiers_sin_azucares_cuando_sugars_lte_0_5() {
        product.setServingSizeG(BigDecimal.valueOf(30));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of(ingredientWithWeight(100)));
        when(nutritionCalculator.calculate(any(), any(), any()))
                .thenReturn(nutritionResult(0, 0, 0, 0.4, 0, 0, 0, 0)); // sugars 0.4 g

        List<String> mods = service.buildNutritionalModifiers(PRODUCT_ID, TENANT_ID, product);
        assertThat(mods).contains("Sin azúcares");
    }

    @Test
    void buildNutritionalModifiers_alto_en_proteinas_solido() {
        product.setServingSizeG(BigDecimal.valueOf(30));
        product.setWeightUnit("g"); // sólido
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of(ingredientWithWeight(100)));
        when(nutritionCalculator.calculate(any(), any(), any()))
                .thenReturn(nutritionResult(0, 22, 0, 0, 0, 0, 0, 0)); // proteínas 22g

        List<String> mods = service.buildNutritionalModifiers(PRODUCT_ID, TENANT_ID, product);
        assertThat(mods).contains("Alto en proteínas");
    }

    @Test
    void buildNutritionalModifiers_sin_grasas_cuando_fat_total_lte_0_5() {
        product.setServingSizeG(BigDecimal.valueOf(30));
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(any(), any()))
                .thenReturn(List.of(ingredientWithWeight(100)));
        when(nutritionCalculator.calculate(any(), any(), any()))
                .thenReturn(nutritionResult(0, 0, 0, 0, 0.3, 0, 0, 0)); // fat 0.3g

        List<String> mods = service.buildNutritionalModifiers(PRODUCT_ID, TENANT_ID, product);
        assertThat(mods).contains("Sin grasas");
    }

    // ── Claims de enriquecimiento ─────────────────────────────────────────────

    @Test
    void buildEnrichmentClaims_hierro_detectado() {
        List<Ingredient> ings = List.of(
                ingredientNamed("Sulfato ferroso (hierro)"),
                ingredientNamed("Harina de trigo")
        );
        List<String> claims = service.buildEnrichmentClaims(ings);
        assertThat(claims).contains("Enriquecido con hierro");
    }

    @Test
    void buildEnrichmentClaims_calcio_detectado() {
        List<Ingredient> ings = List.of(ingredientNamed("Carbonato de calcio"));
        assertThat(service.buildEnrichmentClaims(ings)).contains("Enriquecido con calcio");
    }

    @Test
    void buildEnrichmentClaims_vitaminas_sin_especifico() {
        List<Ingredient> ings = List.of(ingredientNamed("Vitamina D3"));
        assertThat(service.buildEnrichmentClaims(ings)).contains("Enriquecido con vitaminas");
    }

    @Test
    void buildEnrichmentClaims_vitaminas_con_especifico_no_agrega_generico() {
        List<Ingredient> ings = List.of(ingredientNamed("Vitamina D3"), ingredientNamed("Hierro"));
        List<String> claims = service.buildEnrichmentClaims(ings);
        assertThat(claims).contains("Enriquecido con hierro");
        assertThat(claims).doesNotContain("Enriquecido con vitaminas");
    }

    @Test
    void buildEnrichmentClaims_sin_micronutrientes_retorna_lista_vacia() {
        List<Ingredient> ings = List.of(ingredientNamed("Harina de trigo"), ingredientNamed("Agua"));
        assertThat(service.buildEnrichmentClaims(ings)).isEmpty();
    }

    // ── Helpers de test ───────────────────────────────────────────────────────

    private Ingredient ingredientWithWeight(double weight) {
        Ingredient i = new Ingredient();
        i.setId(UUID.randomUUID());
        i.setProductId(PRODUCT_ID);
        i.setTenantId(TENANT_ID);
        i.setName("Ingrediente genérico");
        i.setWeightGrams(BigDecimal.valueOf(weight));
        i.setAllergen(false);
        return i;
    }

    private Ingredient ingredientNamed(String name) {
        Ingredient i = ingredientWithWeight(100);
        i.setName(name);
        return i;
    }

    private NutritionCalculationResult nutritionResult(
            double energy, double proteins, double carbs, double sugars,
            double fat, double fatSat, double fatTrans, double sodium) {
        NutritionValues raw = new NutritionValues(energy, energy * 4.184, proteins, carbs, sugars, fat, fatSat, fatTrans, sodium);
        RoundedNutritionValues rounded = new RoundedNutritionValues(
                (int) energy, (int)(energy * 4.184), (int) proteins, (int) carbs, (int) sugars,
                (int) fat, (int) fatSat, fatTrans, (int) sodium);
        return new NutritionCalculationResult(BigDecimal.valueOf(30), rounded, rounded, raw);
    }
}
