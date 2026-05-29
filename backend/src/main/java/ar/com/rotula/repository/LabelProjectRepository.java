package ar.com.rotula.repository;

import ar.com.rotula.domain.LabelProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelProjectRepository extends JpaRepository<LabelProject, UUID> {

    List<LabelProject> findByTenantId(UUID tenantId);

    List<LabelProject> findByProductIdAndTenantIdOrderByVersionDesc(UUID productId, UUID tenantId);

    @Query("SELECT MAX(lp.version) FROM LabelProject lp WHERE lp.productId = :productId AND lp.tenantId = :tenantId")
    Optional<Integer> findMaxVersionByProductIdAndTenantId(
            @Param("productId") UUID productId,
            @Param("tenantId") UUID tenantId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, String status);

    List<LabelProject> findTop5ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
