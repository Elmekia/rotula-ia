package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.domain.Product;
import ar.com.rotula.domain.RegulatoryName;
import ar.com.rotula.dto.LegalNameSuggestion;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.repository.RegulatoryNameRepository;
import ar.com.rotula.security.AppUserDetails;
import ar.com.rotula.service.nutrition.NutritionCalculationResult;
import ar.com.rotula.service.nutrition.NutritionCalculatorService;
import ar.com.rotula.service.nutrition.NutritionValues;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sugiere la denominación legal de venta de un producto según su categoría
 * y composición, conforme al CAA y Ley 27.642.
 *
 * <p>La sugerencia incluye:
 * <ul>
 *   <li>Nombre legal base (lookup en {@code regulatory_name} por categoría)</li>
 *   <li>Modificadores / claims nutricionales (CAA Art. 1385): bajo en, sin, enriquecido con</li>
 *   <li>Alerta si la denominación comercial difiere de la legal</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class LegalNameService {

    private final ProductRepository           productRepository;
    private final IngredientRepository        ingredientRepository;
    private final RegulatoryNameRepository    regulatoryNameRepository;
    private final NutritionCalculatorService  nutritionCalculator;

    @Transactional(readOnly = true)
    public LegalNameSuggestion suggest(UUID productId) {
        AppUserDetails user = currentUser();
        UUID tenantId = user.getTenantId();
        return suggest(productId, tenantId);
    }

    /**
     * Variante con tenantId explícito (usada por {@code LabelGenerationService} para
     * evitar una segunda lectura del SecurityContext).
     */
    @Transactional(readOnly = true)
    public LegalNameSuggestion suggest(UUID productId, UUID tenantId) {
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        // 1. Lookup de denominación legal por denominación del producto
        // La denominación del alimento (ingresada por el usuario) se usa como
        // clave de búsqueda; si no hay coincidencia exacta, se retorna sin nombre legal.
        String denomKey = product.getDenomination() != null
                ? product.getDenomination().trim()
                : product.getName().trim();
        Optional<RegulatoryName> rn = regulatoryNameRepository
                .findByCategoryIgnoreCaseAndActiveTrue(denomKey);

        if (rn.isEmpty()) {
            return new LegalNameSuggestion(null, List.of(), false, null);
        }

        RegulatoryName regulatory = rn.get();

        // 2. Calcular modificadores nutricionales (CAA Art. 1385)
        List<String> modifiers = buildNutritionalModifiers(productId, tenantId, product);

        // 3. Claims de enriquecimiento según ingredientes
        List<Ingredient> ingredients = ingredientRepository
                .findByProductIdAndTenantIdOrderByWeightGramsDesc(productId, tenantId);
        List<String> enrichmentClaims = buildEnrichmentClaims(ingredients);

        List<String> allModifiers = new ArrayList<>(modifiers);
        allModifiers.addAll(enrichmentClaims);

        // 4. Alerta si la denominación comercial difiere de la legal
        boolean alert = !productNameMatchesLegal(product.getName(), regulatory.getBaseName());

        return new LegalNameSuggestion(
                regulatory.getBaseName(),
                List.copyOf(allModifiers),
                alert,
                regulatory.getSourceArticle()
        );
    }

    // ── Modificadores nutricionales (CAA Art. 1385) ───────────────────────────

    /**
     * Evalúa qué claims nutricionales ("bajo en", "sin") aplican al producto
     * según los umbrales del CAA Art. 1385.
     *
     * <p>Los claims "sin" tienen prioridad sobre "bajo en" para el mismo nutriente.
     */
    List<String> buildNutritionalModifiers(UUID productId, UUID tenantId, Product product) {
        // La nutrición ahora vive en el producto; calculamos directo desde sus campos.
        NutritionCalculationResult result = nutritionCalculator.calculate(product);
        if (result == null) return List.of();

        NutritionValues raw = result.rawPer100g();
        boolean isLiquid   = isLiquid(product.getWeightUnit());

        List<String> modifiers = new ArrayList<>();

        // Grasas totales
        double fatLimit      = isLiquid ? 1.5 : 3.0;
        if (raw.fatTotalG() <= 0.5)             modifiers.add("Sin grasas");
        else if (raw.fatTotalG() < fatLimit)    modifiers.add("Bajo en grasas");

        // Grasa saturada
        double satLimit      = isLiquid ? 0.75 : 1.5;
        if (raw.fatSatG() < satLimit)           modifiers.add("Bajo en grasa saturada");

        // Grasa trans
        if (raw.fatTransG() <= 0.1)             modifiers.add("Sin grasas trans");

        // Sodio
        if (raw.sodiumMg() <= 5.0)             modifiers.add("Sin sodio");
        else if (raw.sodiumMg() < 120.0)        modifiers.add("Bajo en sodio");

        // Azúcares
        double sugarLimit    = isLiquid ? 2.5 : 5.0;
        if (raw.sugarsG() <= 0.5)              modifiers.add("Sin azúcares");
        else if (raw.sugarsG() < sugarLimit)   modifiers.add("Bajo en azúcares");

        // Calorías
        double energyLimit   = isLiquid ? 20.0 : 40.0;
        if (raw.energyKcal() <= 4.0)           modifiers.add("Sin calorías");
        else if (raw.energyKcal() < energyLimit) modifiers.add("Bajo en calorías");

        // Proteínas (claim positivo)
        double proteinThreshold = isLiquid ? 10.0 : 20.0;
        if (raw.proteinsG() >= proteinThreshold) modifiers.add("Alto en proteínas");

        return modifiers;
    }

    // ── Enriquecimiento ───────────────────────────────────────────────────────

    /**
     * Detecta claims de enriquecimiento a partir de los nombres de ingredientes.
     * Los micronutrientes añadidos deben declararse explícitamente en la lista de ingredientes.
     */
    List<String> buildEnrichmentClaims(List<Ingredient> ingredients) {
        List<String> claims = new ArrayList<>();
        boolean hasVitamins = false;

        for (Ingredient i : ingredients) {
            String n = i.getName().toLowerCase();
            if (n.contains("hierro"))                          { claims.add("Enriquecido con hierro"); }
            else if (n.contains("calcio"))                     { claims.add("Enriquecido con calcio"); }
            else if (n.contains("zinc"))                       { claims.add("Enriquecido con zinc"); }
            else if (n.contains("fólico") || n.contains("folico")) { claims.add("Enriquecido con ácido fólico"); }
            else if (n.contains("omega"))                      { claims.add("Enriquecido con ácidos grasos omega"); }
            else if (n.contains("vitamina"))                   { hasVitamins = true; }
        }

        // Añadir claim de vitaminas solo si no hay un micronutriente específico ya listado
        if (hasVitamins && claims.isEmpty()) {
            claims.add("Enriquecido con vitaminas");
        }

        return claims;
    }

    // ── Alert ─────────────────────────────────────────────────────────────────

    /**
     * Verifica si el nombre comercial del producto contiene al menos una palabra
     * significativa (>3 caracteres) de la denominación legal.
     *
     * <p>La comparación se hace sin acentos y en minúsculas.
     */
    boolean productNameMatchesLegal(String productName, String legalName) {
        if (productName == null || legalName == null) return true;
        String pn = normalize(productName);
        String ln = normalize(legalName);
        return java.util.Arrays.stream(ln.split("\\s+"))
                .filter(w -> w.length() > 3)
                .anyMatch(pn::contains);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isLiquid(String weightUnit) {
        return "l".equals(weightUnit) || "ml".equals(weightUnit) || "cc".equals(weightUnit);
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .toLowerCase();
    }

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
