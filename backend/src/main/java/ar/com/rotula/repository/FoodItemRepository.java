package ar.com.rotula.repository;

import ar.com.rotula.domain.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FoodItemRepository extends JpaRepository<FoodItem, UUID> {

    /** Todos los alimentos activos de un grupo, ordenados por sort_order. */
    List<FoodItem> findByFoodGroupIdAndActiveTrueOrderBySortOrderAsc(UUID foodGroupId);
}
