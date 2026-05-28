package ar.com.rotula.service;

import ar.com.rotula.domain.Ingredient;
import ar.com.rotula.dto.IngredientRequest;
import ar.com.rotula.dto.IngredientResponse;
import ar.com.rotula.exception.PercentageSumExceededException;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.IngredientRepository;
import ar.com.rotula.repository.ProductRepository;
import ar.com.rotula.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100.000");

    private final IngredientRepository ingredientRepository;
    private final ProductRepository    productRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<IngredientResponse> findByProduct(UUID productId) {
        AppUserDetails user = currentUser();
        requireProductOwned(productId, user.getTenantId());
        return ingredientRepository
                .findByProductIdAndTenantIdOrderBySortOrder(productId, user.getTenantId())
                .stream()
                .map(i -> new IngredientResponse(
                        i.getId(),
                        i.getProductId(),
                        i.getTenantId(),
                        i.getName(),
                        i.getPercentage(),
                        AllergenDetector.isAllergen(i.getName()),   // re-detect on every list load
                        i.getSortOrder(),
                        i.getCreatedAt()
                ))
                .toList();
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public IngredientResponse create(UUID productId, IngredientRequest req) {
        AppUserDetails user = currentUser();
        requireProductOwned(productId, user.getTenantId());

        // Synthetic UUID as exclude (no existing ingredient)
        UUID noExclude = new UUID(0, 0);
        validatePercentageSum(productId, user.getTenantId(), noExclude, req.percentage());

        boolean allergen = resolveAllergen(req);

        // Auto-assign sort_order if not meaningful (client may send max+1 or use backend default)
        int sortOrder = req.sortOrder() >= 0
                ? req.sortOrder()
                : ingredientRepository.maxSortOrder(productId, user.getTenantId()) + 1;

        Ingredient saved = ingredientRepository.save(Ingredient.builder()
                .productId(productId)
                .tenantId(user.getTenantId())
                .name(req.name())
                .percentage(req.percentage())
                .allergen(allergen)
                .sortOrder(sortOrder)
                .build());

        return IngredientResponse.from(saved);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public IngredientResponse update(UUID ingredientId, IngredientRequest req) {
        AppUserDetails user = currentUser();
        Ingredient ingredient = requireIngredientOwned(ingredientId, user.getTenantId());

        validatePercentageSum(ingredient.getProductId(), user.getTenantId(), ingredientId, req.percentage());

        ingredient.setName(req.name());
        ingredient.setPercentage(req.percentage());
        ingredient.setAllergen(resolveAllergen(req));
        ingredient.setSortOrder(req.sortOrder());

        return IngredientResponse.from(ingredientRepository.save(ingredient));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID ingredientId) {
        Ingredient ingredient = requireIngredientOwned(ingredientId, currentUser().getTenantId());
        ingredientRepository.delete(ingredient);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Validates that adding {@code incoming} percentage (excluding ingredient {@code excludeId})
     * doesn't push the total over 100%.
     */
    private void validatePercentageSum(UUID productId, UUID tenantId, UUID excludeId, BigDecimal incoming) {
        BigDecimal existing = ingredientRepository.sumPercentageExcluding(productId, tenantId, excludeId);
        if (existing.add(incoming).compareTo(MAX_PERCENTAGE) > 0) {
            throw new PercentageSumExceededException(existing, incoming);
        }
    }

    /**
     * If the request provides an explicit allergen flag, use it;
     * otherwise auto-detect from the ingredient name.
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
