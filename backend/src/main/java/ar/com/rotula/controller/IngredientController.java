package ar.com.rotula.controller;

import ar.com.rotula.dto.IngredientRequest;
import ar.com.rotula.dto.IngredientResponse;
import ar.com.rotula.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    /** Lista ingredientes de un producto, ordenados por sort_order. */
    @GetMapping("/products/{productId}/ingredients")
    public List<IngredientResponse> list(@PathVariable UUID productId) {
        return ingredientService.findByProduct(productId);
    }

    /** Agrega un ingrediente al producto. */
    @PostMapping("/products/{productId}/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse create(
            @PathVariable UUID productId,
            @Valid @RequestBody IngredientRequest req
    ) {
        return ingredientService.create(productId, req);
    }

    /** Actualiza un ingrediente (nombre, porcentaje, alérgeno, orden). */
    @PutMapping("/ingredients/{id}")
    public IngredientResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody IngredientRequest req
    ) {
        return ingredientService.update(id, req);
    }

    /** Elimina un ingrediente. */
    @DeleteMapping("/ingredients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        ingredientService.delete(id);
    }
}
