package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.dto.IngredientRequest;
import ar.com.rotula.dto.IngredientResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final ProductRepository    productRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IngredientResponse> findByProduct(UUID productId) {
        AppUserDetails user = currentUser();
        requireProductOwned(productId, user.getTenantId());

        List<Ingredient> ingredients = ingredientRepository
                .findByProductIdAndTenantIdOrderByWeightGramsDesc(productId, user.getTenantId());

        // Calcular total de pesos para derivar % de cada ingrediente
        BigDecimal total = ingredients.stream()
                .map(Ingredient::getWeightGrams)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ingredients.stream()
                .map(i -> toResponse(i, total))
                .toList();
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public IngredientResponse create(UUID productId, IngredientRequest req) {
        AppUserDetails user = currentUser();
        requireProductOwned(productId, user.getTenantId());

        Ingredient saved = ingredientRepository.save(Ingredient.builder()
                .productId(productId)
                .tenantId(user.getTenantId())
                .name(req.name())
                .weightGrams(req.weightGrams())
                .allergen(resolveAllergen(req))
                .build());

        // Calcular % sobre el total actualizado (incluye el ingrediente recién guardado)
        BigDecimal total = ingredientRepository.sumWeightGrams(productId, user.getTenantId());
        return toResponse(saved, total);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public IngredientResponse update(UUID ingredientId, IngredientRequest req) {
        AppUserDetails user = currentUser();
        Ingredient ingredient = requireIngredientOwned(ingredientId, user.getTenantId());

        ingredient.setName(req.name());
        ingredient.setWeightGrams(req.weightGrams());
        ingredient.setAllergen(resolveAllergen(req));

        Ingredient saved = ingredientRepository.save(ingredient);

        BigDecimal total = ingredientRepository.sumWeightGrams(saved.getProductId(), user.getTenantId());
        return toResponse(saved, total);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID ingredientId) {
        Ingredient ingredient = requireIngredientOwned(ingredientId, currentUser().getTenantId());
        ingredientRepository.delete(ingredient);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Construye un IngredientResponse calculando el porcentaje como:
     *   percentage = weightGrams / total * 100
     * Si total es cero (sin ingredientes), el porcentaje es 0.
     */
    private IngredientResponse toResponse(Ingredient i, BigDecimal total) {
        BigDecimal pct = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : i.getWeightGrams()
                        .divide(total, 7, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(3, RoundingMode.HALF_UP);

        return new IngredientResponse(
                i.getId(),
                i.getProductId(),
                i.getTenantId(),
                i.getName(),
                i.getWeightGrams(),
                pct,
                AllergenDetector.isAllergen(i.getName()),
                i.getCreatedAt()
        );
    }

    /**
     * Si el request provee un flag explícito de alérgeno, se usa ese valor;
     * de lo contrario se auto-detecta desde el nombre según Res. 109/2023.
     */
    private boolean resolveAllergen(IngredientRequest req) {
        return req.allergen() != null
                ? req.allergen()
                : AllergenDetector.isAllergen(req.name());
    }

    private void requireProductOwned(UUID productId, UUID tenantId) {
        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productId));
    }

    private Ingredient requireIngredientOwned(UUID ingredientId, UUID tenantId) {
        return ingredientRepository.findByIdAndTenantId(ingredientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + ingredientId));
    }

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
