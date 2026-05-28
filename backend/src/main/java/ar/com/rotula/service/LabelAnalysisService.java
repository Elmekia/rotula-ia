package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.domain.Product;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import ar.com.rotula.service.allergen.AllergenDeclarationResult;
import ar.com.rotula.service.allergen.AllergenDeclarationService;
import ar.com.rotula.service.nutrition.NutritionCalculationResult;
import ar.com.rotula.service.nutrition.NutritionCalculatorService;
import ar.com.rotula.service.nutrition.NutritionValues;
import ar.com.rotula.service.nutrition.SealEvaluatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orquesta el análisis completo del rótulo de un producto:
 * <ol>
 *   <li>Tabla nutricional según CAA Art. 1354</li>
 *   <li>Sellos de advertencia según Ley 27.642 / Decreto 151/2022</li>
 *   <li>Declaración de alérgenos según Res. 109/2023</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class LabelAnalysisService {

    private final ProductRepository           productRepository;
    private final IngredientRepository        ingredientRepository;
    private final NutritionCalculatorService  nutritionCalculator;
    private final SealEvaluatorService        sealEvaluator;
    private final AllergenDeclarationService  allergenDeclaration;

    @Transactional(readOnly = true)
    public LabelAnalysisResult analyze(UUID productId) {
        AppUserDetails user = currentUser();
        UUID tenantId = user.getTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        List<Ingredient> ingredients = ingredientRepository
                .findByProductIdAndTenantIdOrderByWeightGramsDesc(productId, tenantId);

        // ── Tabla nutricional ─────────────────────────────────────────────────
        NutritionCalculationResult nutrition = null;
        Set<String> seals = Set.of();

        if (product.getServingSizeG() != null && !ingredients.isEmpty()) {
            BigDecimal totalWeight = ingredients.stream()
                    .map(Ingredient::getWeightGrams)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            nutrition = nutritionCalculator.calculate(ingredients, totalWeight, product.getServingSizeG());

            // ── Sellos de advertencia ─────────────────────────────────────────
            if (nutrition != null) {
                boolean isLiquid = isLiquidProduct(product.getWeightUnit());
                NutritionValues raw100g = nutrition.rawPer100g();
                seals = sealEvaluator.evaluate(raw100g, isLiquid);
            }
        }

        // ── Declaración de alérgenos ──────────────────────────────────────────
        List<String> ingredientNames = ingredients.stream()
                .map(Ingredient::getName)
                .toList();
        AllergenDeclarationResult allergens = allergenDeclaration.buildDeclaration(
                ingredientNames,
                product.getCrossContamination()
        );

        return new LabelAnalysisResult(productId, nutrition, seals, allergens);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Un producto es líquido si su unidad de peso es litros, mililitros o cc.
     * Esto determina qué umbrales de Ley 27.642 se aplican.
     */
    private static boolean isLiquidProduct(String weightUnit) {
        return "l".equals(weightUnit)
                || "ml".equals(weightUnit)
                || "cc".equals(weightUnit);
    }

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
