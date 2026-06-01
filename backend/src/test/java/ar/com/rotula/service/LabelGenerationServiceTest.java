package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.domain.LabelProject;
import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.LegalNameSuggestion;
import ar.com.rotula.dto.label.LabelVersionResponse;
import ar.com.rotula.dto.label.LabelVersionSummary;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.LabelProjectRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import ar.com.rotula.service.allergen.AllergenDeclarationResult;
import ar.com.rotula.service.nutrition.NutritionCalculationResult;
import ar.com.rotula.service.nutrition.NutritionValues;
import ar.com.rotula.service.nutrition.RoundedNutritionValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelGenerationServiceTest {

    @Mock ProductRepository      productRepository;
    @Mock IngredientRepository   ingredientRepository;
    @Mock LabelProjectRepository labelProjectRepository;
    @Mock LegalNameService       legalNameService;
    @Mock LabelAnalysisService   labelAnalysisService;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks LabelGenerationService service;

    private static final UUID TENANT_ID  = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID PRODUCT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID USER_ID    = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    @BeforeEach
    void authenticate() {
        AppUserDetails user = new AppUserDetails(USER_ID, TENANT_ID, "user@test.com", "hash", "user");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // ── generate() ────────────────────────────────────────────────────────────

    @Test
    void generate_primera_version_asigna_version_1() {
        mockBaseData();
        mockDefaultLegalName();
        when(labelProjectRepository.findMaxVersionByProductIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty()); // no hay versiones previas

        LabelVersionResponse response = service.generate(PRODUCT_ID);

        assertThat(response.version()).isEqualTo(1);
    }

    @Test
    void generate_segunda_version_es_2() {
        mockBaseData();
        mockDefaultLegalName();
        when(labelProjectRepository.findMaxVersionByProductIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(1));

        LabelVersionResponse response = service.generate(PRODUCT_ID);

        assertThat(response.version()).isEqualTo(2);
    }

    @Test
    void generate_incluye_productId_y_nombre_en_dto() {
        mockBaseData();
        mockDefaultLegalName();
        when(labelProjectRepository.findMaxVersionByProductIdAndTenantId(any(), any()))
                .thenReturn(Optional.empty());

        LabelVersionResponse response = service.generate(PRODUCT_ID);

        assertThat(response.labelData().productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.labelData().productName()).isEqualTo("Producto Test");
    }

    @Test
    void generate_alerta_legal_propagada_al_dto() {
        mockBaseData();
        when(legalNameService.suggest(eq(PRODUCT_ID), eq(TENANT_ID)))
                .thenReturn(new LegalNameSuggestion("Nombre Legal", List.of(), true, "CAA"));
        when(labelProjectRepository.findMaxVersionByProductIdAndTenantId(any(), any()))
                .thenReturn(Optional.empty());

        LabelVersionResponse response = service.generate(PRODUCT_ID);

        assertThat(response.labelData().legalNameAlert()).isTrue();
    }

    @Test
    void generate_claims_propagados_desde_legal_name() {
        mockBaseData();
        when(legalNameService.suggest(eq(PRODUCT_ID), eq(TENANT_ID)))
                .thenReturn(new LegalNameSuggestion("Leche", List.of("Bajo en sodio", "Sin grasas"), false, "CAA Art. 559"));
        when(labelProjectRepository.findMaxVersionByProductIdAndTenantId(any(), any()))
                .thenReturn(Optional.empty());

        LabelVersionResponse response = service.generate(PRODUCT_ID);

        assertThat(response.labelData().claims()).containsExactlyInAnyOrder("Bajo en sodio", "Sin grasas");
    }

    @Test
    void generate_producto_no_existente_lanza_exception() {
        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(PRODUCT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generate_persiste_label_project_con_status_draft() {
        mockBaseData();
        mockDefaultLegalName();
        when(labelProjectRepository.findMaxVersionByProductIdAndTenantId(any(), any()))
                .thenReturn(Optional.empty());

        service.generate(PRODUCT_ID);

        verify(labelProjectRepository).save(argThat(lp ->
                "draft".equals(lp.getStatus())
                && PRODUCT_ID.equals(lp.getProductId())
                && TENANT_ID.equals(lp.getTenantId())
                && lp.getLabelData() != null
                && !lp.getLabelData().isBlank()
        ));
    }

    // ── getVersions() ─────────────────────────────────────────────────────────

    @Test
    void getVersions_retorna_lista_ordenada_desc() {
        LabelProject v2 = buildLabelProject(2);
        LabelProject v1 = buildLabelProject(1);
        when(labelProjectRepository.findByProductIdAndTenantIdOrderByVersionDesc(PRODUCT_ID, TENANT_ID))
                .thenReturn(List.of(v2, v1));

        List<LabelVersionSummary> versions = service.getVersions(PRODUCT_ID);

        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).version()).isEqualTo(2);
        assertThat(versions.get(1).version()).isEqualTo(1);
    }

    @Test
    void getVersions_sin_versiones_retorna_lista_vacia() {
        when(labelProjectRepository.findByProductIdAndTenantIdOrderByVersionDesc(PRODUCT_ID, TENANT_ID))
                .thenReturn(List.of());

        assertThat(service.getVersions(PRODUCT_ID)).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mockBaseData() {
        Product product = Product.builder()
                .id(PRODUCT_ID).tenantId(TENANT_ID)
                .name("Producto Test").denomination("Galletitas")
                .netWeight(BigDecimal.valueOf(500)).weightUnit("g")
                .status("active")
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();

        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                .thenReturn(Optional.of(product));

        Ingredient ing = new Ingredient();
        ing.setId(UUID.randomUUID());
        ing.setProductId(PRODUCT_ID);
        ing.setTenantId(TENANT_ID);
        ing.setName("Harina de trigo");
        ing.setWeightGrams(BigDecimal.valueOf(300));
        ing.setAllergen(true);
        when(ingredientRepository.findByProductIdAndTenantIdOrderByWeightGramsDesc(PRODUCT_ID, TENANT_ID))
                .thenReturn(List.of(ing));

        NutritionValues raw = new NutritionValues(370, 1548, 7, 80, 30, 1, 0.1, 0.0, 150);
        RoundedNutritionValues rounded = new RoundedNutritionValues(370, 1550, 7, 80, 30, 1, 0, 0.0, 150);
        NutritionCalculationResult nutrition = new NutritionCalculationResult(
                BigDecimal.valueOf(30), rounded, rounded, raw);

        AllergenDeclarationResult allergens = new AllergenDeclarationResult(
                EnumSet.noneOf(ar.com.rotula.domain.AllergenGroup.class),
                EnumSet.noneOf(ar.com.rotula.domain.AllergenGroup.class), "");

        LabelAnalysisResult analysis = new LabelAnalysisResult(PRODUCT_ID, nutrition, Set.of(), allergens);
        when(labelAnalysisService.analyze(PRODUCT_ID)).thenReturn(analysis);

        // Save returns label project with generated ID and timestamps
        when(labelProjectRepository.save(any())).thenAnswer(inv -> {
            LabelProject lp = inv.getArgument(0);
            lp.setCreatedAt(OffsetDateTime.now());
            lp.setUpdatedAt(OffsetDateTime.now());
            return lp;
        });
    }

    private void mockDefaultLegalName() {
        when(legalNameService.suggest(eq(PRODUCT_ID), eq(TENANT_ID)))
                .thenReturn(new LegalNameSuggestion("Galletita", List.of(), false, "CAA Art. 752"));
    }

    private LabelProject buildLabelProject(int version) {
        LabelProject lp = new LabelProject();
        lp.setId(UUID.randomUUID());
        lp.setProductId(PRODUCT_ID);
        lp.setTenantId(TENANT_ID);
        lp.setVersion(version);
        lp.setStatus("draft");
        lp.setLegalDenomination("Galletita");
        lp.setCreatedAt(OffsetDateTime.now());
        lp.setUpdatedAt(OffsetDateTime.now());
        return lp;
    }
}
