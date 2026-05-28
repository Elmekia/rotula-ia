package ar.com.rotula.repository;

import ar.com.rotula.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    List<Ingredient> findByProductIdOrderBySortOrder(UUID productId);

    List<Ingredient> findByProductIdAndTenantIdOrderBySortOrder(UUID productId, UUID tenantId);

    List<Ingredient> findByTenantId(UUID tenantId);

    Optional<Ingredient> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Suma los porcentajes de todos los ingredientes de un producto, excluyendo uno (para updates). */
    @Query("""
            SELECT COALESCE(SUM(i.percentage), 0)
            FROM Ingredient i
            WHERE i.productId = :productId
              AND i.tenantId  = :tenantId
              AND i.id       != :excludeId
            """)
    BigDecimal sumPercentageExcluding(
            @Param("productId")  UUID productId,
            @Param("tenantId")   UUID tenantId,
            @Param("excludeId")  UUID excludeId);

    /** Máximo sort_order actual del producto (para auto-asignar el siguiente). */
    @Query("SELECT COALESCE(MAX(i.sortOrder), -1) FROM Ingredient i WHERE i.productId = :productId AND i.tenantId = :tenantId")
    int maxSortOrder(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
}
