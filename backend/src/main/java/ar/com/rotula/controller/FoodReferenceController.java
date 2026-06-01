package ar.com.rotula.controller;

import ar.com.rotula.domain.FoodGroup;
import ar.com.rotula.domain.FoodItem;
import ar.com.rotula.dto.FoodGroupResponse;
import ar.com.rotula.dto.FoodItemResponse;
import ar.com.rotula.exception.ResourceNotFoundException;
import ar.com.rotula.repository.FoodGroupRepository;
import ar.com.rotula.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de datos de referencia de la TABLA I (Res. Conjunta 21/2023).
 * Son datos globales (no tenant-específicos) que el frontend necesita para
 * armar la cascada Grupo → Alimento → Porción en el formulario de producto.
 *
 * Requiere autenticación (cualquier usuario autenticado puede leerlos).
 */
@RestController
@RequestMapping("/food-reference")
@RequiredArgsConstructor
public class FoodReferenceController {

    private final FoodGroupRepository foodGroupRepository;
    private final FoodItemRepository  foodItemRepository;

    /** Lista todos los grupos de alimentos activos (TABLA I). */
    @GetMapping("/groups")
    public List<FoodGroupResponse> listGroups() {
        return foodGroupRepository
                .findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(FoodGroupResponse::from)
                .toList();
    }

    /** Lista los alimentos activos de un grupo, con sus porciones de referencia. */
    @GetMapping("/groups/{groupId}/items")
    public List<FoodItemResponse> listItems(@PathVariable UUID groupId) {
        // Validamos que el grupo exista
        foodGroupRepository.findById(groupId)
                .filter(FoodGroup::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de alimentos no encontrado: " + groupId));

        return foodItemRepository
                .findByFoodGroupIdAndActiveTrueOrderBySortOrderAsc(groupId)
                .stream()
                .map(FoodItemResponse::from)
                .toList();
    }

    /** Devuelve un alimento específico (útil para pre-cargar el formulario al editar). */
    @GetMapping("/items/{itemId}")
    public ResponseEntity<FoodItemResponse> getItem(@PathVariable UUID itemId) {
        return foodItemRepository.findById(itemId)
                .filter(FoodItem::isActive)
                .map(FoodItemResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Alimento no encontrado: " + itemId));
    }
}
