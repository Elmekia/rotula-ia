package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Regla normativa del CAA / Ley 27.642 versionada.
 *
 * Cada regla describe una obligación de etiquetado para una categoría de
 * producto. La condición de aplicabilidad se almacena como JSON estructurado
 * en {@code conditionDsl} y es evaluada en tiempo de generación del rótulo.
 *
 * @see <a href="https://www.argentina.gob.ar/anmat/caa">CAA vigente</a>
 * @see <a href="https://www.argentina.gob.ar/normativa/nacional/ley-27642">Ley 27.642</a>
 */
@Entity
@Table(name = "regulatory_rule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegulatoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Código único. Formato: {CATEGORIA}-{NNN}, ej.: BEB-001, SNK-004. */
    @Column(nullable = false, length = 20)
    private String code;

    /** Categoría de producto. NULL = aplica a todas las categorías. */
    @Column(length = 50)
    private String category;

    /** Descripción legible en español de la obligación normativa. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Condición de aplicabilidad como JSON estructurado (DSL interno).
     * <pre>
     * Ejemplos:
     *   {"category":["bebidas"]}
     *   {"ingredientNameContains":"cafeína"}
     *   {"nutrient":"sodium_mg_per_100g","operator":"gte","threshold":400}
     * </pre>
     */
    @Column(name = "condition_dsl", nullable = false, columnDefinition = "TEXT")
    private String conditionDsl;

    /**
     * Tipo de acción que impone la regla. Valores posibles:
     * DECLARAR_LEYENDA_OBLIGATORIA, INCLUIR_SELLO_ADVERTENCIA,
     * VERIFICAR_DENOMINACION_LEGAL, DECLARAR_COMPOSICION,
     * ADVERTENCIA_ALERGENO, DECLARAR_ADITIVO, DECLARAR_NUTRIENTE,
     * VERIFICAR_COMPOSICION
     */
    @Column(nullable = false, length = 50)
    private String action;

    /** Detalle de la acción: texto exacto de la leyenda, nombre del sello, etc. */
    @Column(name = "action_detail", columnDefinition = "TEXT")
    private String actionDetail;

    /** Artículo(s) fuente en el cuerpo normativo (CAA, Res., Ley). */
    @Column(name = "source_article", nullable = false, length = 300)
    private String sourceArticle;

    /** Inicio de vigencia de esta versión de la regla. */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /** Fin de vigencia. NULL = vigente indefinidamente. */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    /** Número de versión para trazabilidad de cambios normativos. */
    @Column(nullable = false)
    private int version;

    /** Indica si la versión está activa o archivada. */
    @Column(nullable = false)
    private boolean active;

    /** Notas y justificación del especialista en alimentos. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
