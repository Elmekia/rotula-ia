package ar.com.rotula.repository;

import ar.com.rotula.domain.FoodGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FoodGroupRepository extends JpaRepository<FoodGroup, UUID> {

    /** Todos los grupos activos ordenados por sort_order. */
    List<FoodGroup> findByActiveTrueOrderBySortOrderAsc();
}
