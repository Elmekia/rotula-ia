package ar.com.rotula.repository;

import ar.com.rotula.domain.NutritionTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NutritionTableRepository extends JpaRepository<NutritionTable, UUID> {
    Optional<NutritionTable> findByLabelId(UUID labelId);
}
