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

    /** Listado ordenado por peso descendente (mayor peso primero, conforme normativa). */
    List<Ingredient> findByProductIdAndTenantIdOrderByWeightGramsDesc(UUID productId, UUID tenantId);

    List<Ingredient> findByTenantId(UUID tenantId);

    Optional<Ingredient> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Suma total de pesos del producto (para calcular porcentajes). */
    @Query("""
            SELECT COALESCE(SUM(i.weightGrams), 0)
            FROM Ingredient i
            WHERE i.productId = :productId
              AND i.tenantId  = :tenantId
            """)
    BigDecimal sumWeightGrams(
            @Param("productId") UUID productId,
            @Param("tenantId")  UUID tenantId);
}
