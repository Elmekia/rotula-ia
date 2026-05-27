package ar.com.rotula.repository;

import ar.com.rotula.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {
    List<Ingredient> findByProductIdOrderBySortOrder(UUID productId);
    List<Ingredient> findByTenantId(UUID tenantId);
}
