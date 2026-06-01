package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.domain.LabelProject;
import ar.com.rotula.domain.Product;
import ar.com.rotula.dto.LegalNameSuggestion;
import ar.com.rotula.dto.label.IngredientItem;
import ar.com.rotula.dto.label.LabelDTO;
import ar.com.rotula.dto.label.LabelVersionResponse;
import ar.com.rotula.dto.label.LabelVersionSummary;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.LabelProjectRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Genera un rótulo completo estructurado ({@link LabelDTO}) para un producto
 * y lo persiste como una nueva versión en {@code label_projects}.
 *
 * <p>El proceso de generación orquesta:
 * <ol>
 *   <li>{@link LegalNameService} — denominación legal + claims nutricionales</li>
 *   <li>{@link LabelAnalysisService} — tabla nutricional, sellos, alérgenos</li>
 * </ol>
 *
 * <p>Tiempo de respuesta objetivo: &lt;2 s para el caso base (sin calcular
 * información nutricional faltante).
 */
@Service
@RequiredArgsConstructor
public class LabelGenerationService {

    private final ProductRepository      productRepository;
    private final IngredientRepository   ingredientRepository;
    private final LabelProjectRepository labelProjectRepository;
    private final LegalNameService       legalNameService;
    private final LabelAnalysisService   labelAnalysisService;
    private final ObjectMapper           objectMapper;

    /**
     * Genera y persiste un nuevo rótulo para el producto indicado.
     *
     * @param productId ID del producto (debe pertenecer al tenant del usuario autenticado).
     * @return versión generada con el contenido completo del rótulo.
     */
    @Transactional
    public LabelVersionResponse generate(UUID productId) {
        AppUserDetails user     = currentUser();
        UUID tenantId           = user.getTenantId();

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));

        List<Ingredient> ingredients = ingredientRepository
                .findByProductIdAndTenantIdOrderByWeightGramsDesc(productId, tenantId);

        // ── 1. Denominación legal + claims ────────────────────────────────────
        LegalNameSuggestion legalName = legalNameService.suggest(productId, tenantId);

        // ── 2. Análisis completo (nutrición + sellos + alérgenos) ─────────────
        LabelAnalysisResult analysis = labelAnalysisService.analyze(productId);

        // ── 3. Construir items de ingredientes ────────────────────────────────
        BigDecimal totalWeight = ingredients.stream()
                .map(Ingredient::getWeightGrams)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<IngredientItem> ingredientItems = ingredients.stream()
                .map(i -> {
                    BigDecimal pct = totalWeight.compareTo(BigDecimal.ZERO) > 0
                            ? i.getWeightGrams()
                                  .divide(totalWeight, 6, RoundingMode.HALF_UP)
                                  .multiply(BigDecimal.valueOf(100))
                                  .setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new IngredientItem(i.getName(), i.getWeightGrams(), pct, i.isAllergen());
                })
                .toList();

        // ── 4. Claims = modificadores de denominación legal ───────────────────
        List<String> claims = legalName.modifiers() != null ? legalName.modifiers() : List.of();

        // ── 5. Armar LabelDTO ─────────────────────────────────────────────────
        LabelDTO labelDTO = new LabelDTO(
                productId,
                product.getName(),
                legalName.suggestedName(),
                product.getDenomination(),
                product.getNetWeight(),
                product.getWeightUnit(),
                product.getRneNumber(),
                product.getRnpaNumber(),
                ingredientItems,
                analysis.nutrition(),
                analysis.seals() != null ? analysis.seals() : Set.of(),
                analysis.allergens(),
                claims,
                legalName.alertDiffers()
        );

        // ── 6. Serializar a JSON ──────────────────────────────────────────────
        String labelJson;
        try {
            labelJson = objectMapper.writeValueAsString(labelDTO);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando LabelDTO", e);
        }

        // ── 7. Versión siguiente ──────────────────────────────────────────────
        int nextVersion = labelProjectRepository
                .findMaxVersionByProductIdAndTenantId(productId, tenantId)
                .map(v -> v + 1)
                .orElse(1);

        // ── 8. Persistir ──────────────────────────────────────────────────────
        LabelProject saved = labelProjectRepository.save(
                LabelProject.builder()
                        .tenantId(tenantId)
                        .productId(productId)
                        .createdBy(user.getId())
                        .version(nextVersion)
                        .status("draft")
                        .legalDenomination(legalName.suggestedName())
                        .labelData(labelJson)
                        .disclaimerAccepted(false)
                        .build()
        );

        return new LabelVersionResponse(
                saved.getId(),
                productId,
                nextVersion,
                saved.getStatus(),
                labelDTO,
                saved.getCreatedAt()
        );
    }

    /**
     * Devuelve el historial de versiones de rótulos para el producto indicado,
     * ordenado de más reciente a más antiguo.
     */
    @Transactional(readOnly = true)
    public List<LabelVersionSummary> getVersions(UUID productId) {
        AppUserDetails user = currentUser();
        UUID tenantId = user.getTenantId();

        return labelProjectRepository
                .findByProductIdAndTenantIdOrderByVersionDesc(productId, tenantId)
                .stream()
                .map(LabelVersionSummary::from)
                .toList();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
