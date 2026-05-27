package ar.com.rotula.repository;

import ar.com.rotula.domain.LabelProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabelProjectRepository extends JpaRepository<LabelProject, UUID> {
    List<LabelProject> findByTenantId(UUID tenantId);
    List<LabelProject> findByProductId(UUID productId);
}
