package ar.com.rotula.service;

import ar.com.rotula.domain.AllergenGroup;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository  productRepository;
    private final FoodGroupRepository foodGroupRepository;
    private final FoodItemRepository  foodItemRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(int page, int size, String sortParam) {
        UUID tenantId = currentUser().getTenantId();
        Sort sort = parseSort(sortParam);
        Pageable pageable = PageRequest.of(page, size, sort);
        return PageResponse.from(
                productRepository.findByTenantId(tenantId, pageable),
                ProductResponse::from
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return ProductResponse.from(requireOwned(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse create(ProductRequest req) {
        AppUserDetails user = currentUser();

        FoodItem foodItem = resolveFoodItem(req.foodGroupId(), req.foodItemId());
        validateCrossContamination(req.crossContaminationGroups());

        Product product = Product.builder()
                .tenantId(user.getTenantId())
                .createdBy(user.getId())
                .name(req.name())
                .denomination(req.denomination())
                .foodGroupId(req.foodGroupId())
                .foodItemId(req.foodItemId())
                .servingSizeG(foodItem.getPortionGrams())   // siempre desde la tabla de referencia
                .netWeight(req.netWeight())
                .weightUnit(req.weightUnit())
                .rneNumber(req.rneNumber())
                .rnpaNumber(req.rnpaNumber())
                .crossContamination(joinCrossContamination(req.crossContaminationGroups()))
                .showIngredientPercentages(req.showIngredientPercentages())
                .energyKcalPer100g(req.energyKcalPer100g())
                .proteinsPer100g(req.proteinsPer100g())
                .carbsPer100g(req.carbsPer100g())
                .sugarsPer100g(req.sugarsPer100g())
                .fatTotalPer100g(req.fatTotalPer100g())
                .fatSatPer100g(req.fatSatPer100g())
                .fatTransPer100g(req.fatTransPer100g())
                .sodiumMgPer100g(req.sodiumMgPer100g())
                .status("draft")
                .build();

        return ProductResponse.from(productRepository.save(product));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse update(UUID id, ProductRequest req) {
        Product product = requireOwned(id);

        FoodItem foodItem = resolveFoodItem(req.foodGroupId(), req.foodItemId());
        validateCrossContamination(req.crossContaminationGroups());

        product.setName(req.name());
        product.setDenomination(req.denomination());
        product.setFoodGroupId(req.foodGroupId());
        product.setFoodItemId(req.foodItemId());
        product.setServingSizeG(foodItem.getPortionGrams());
        product.setNetWeight(req.netWeight());
        product.setWeightUnit(req.weightUnit());
        product.setRneNumber(req.rneNumber());
        product.setRnpaNumber(req.rnpaNumber());
        product.setCrossContamination(joinCrossContamination(req.crossContaminationGroups()));
        product.setShowIngredientPercentages(req.showIngredientPercentages());
        product.setEnergyKcalPer100g(req.energyKcalPer100g());
        product.setProteinsPer100g(req.proteinsPer100g());
        product.setCarbsPer100g(req.carbsPer100g());
        product.setSugarsPer100g(req.sugarsPer100g());
        product.setFatTotalPer100g(req.fatTotalPer100g());
        product.setFatSatPer100g(req.fatSatPer100g());
        product.setFatTransPer100g(req.fatTransPer100g());
        product.setSodiumMgPer100g(req.sodiumMgPer100g());

        return ProductResponse.from(productRepository.save(product));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        productRepository.delete(requireOwned(id));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Valida que el food_item pertenezca al food_group indicado y esté activo.
     * Devuelve el FoodItem para poder extraer portion_grams.
     */
    private FoodItem resolveFoodItem(UUID groupId, UUID itemId) {
        foodGroupRepository.findById(groupId)
                .filter(ar.com.rotula.domain.FoodGroup::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo de alimentos no encontrado: " + groupId));

        FoodItem item = foodItemRepository.findById(itemId)
                .filter(FoodItem::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alimento no encontrado: " + itemId));

        if (!item.getFoodGroupId().equals(groupId)) {
            throw new IllegalArgumentException(
                    "El alimento " + itemId + " no pertenece al grupo " + groupId);
        }
        return item;
    }

    /**
     * Valida que cada string en la lista sea un nombre válido del enum AllergenGroup.
     */
    private void validateCrossContamination(List<String> groups) {
        if (groups == null || groups.isEmpty()) return;
        for (String g : groups) {
            try {
                AllergenGroup.valueOf(g);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Grupo de alérgeno inválido: '" + g + "'. Valores válidos: "
                        + java.util.Arrays.toString(AllergenGroup.values()));
            }
        }
    }

    /** Convierte ["MANI","SOJA"] → "MANI,SOJA" para persistencia. */
    private String joinCrossContamination(List<String> groups) {
        if (groups == null || groups.isEmpty()) return null;
        return groups.stream()
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private Product requireOwned(UUID id) {
        UUID tenantId = currentUser().getTenantId();
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado: " + id));
    }

    private AppUserDetails currentUser() {
        return (AppUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",", 2);
        String field = parts[0].trim();
        Sort.Direction direction = parts.length == 2
                && parts[1].trim().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
