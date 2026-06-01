package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "food_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "food_group_id", nullable = false)
    private UUID foodGroupId;

    /** Nombre descriptivo del alimento (TABLA I). */
    @Column(nullable = false)
    private String name;

    /**
     * Porción de referencia en gramos o mL (según {@code unit}).
     * Proviene de la TABLA I de la Res. Conjunta 21/2023.
     * No es editable por el usuario.
     */
    @Column(name = "portion_grams", nullable = false, precision = 8, scale = 2)
    private BigDecimal portionGrams;

    /** Unidad de la porción: 'g' o 'ml'. */
    @Column(nullable = false, length = 5)
    private String unit;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(nullable = false)
    private boolean active;
}
