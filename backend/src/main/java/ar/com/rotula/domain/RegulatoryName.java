package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Denominación legal de venta reglamentaria por categoría de producto,
 * según el Código Alimentario Argentino (CAA).
 *
 * <p>Esta tabla es de referencia global (no tenant-específica) y contiene
 * el nombre que debe figurar en el rótulo como «denominación de venta»
 * para cada categoría, con su fuente normativa.
 */
@Entity
@Table(name = "regulatory_name")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegulatoryName {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Categoría del producto (en minúsculas), coincide con {@code products.category}. */
    @Column(nullable = false, length = 100)
    private String category;

    /** Denominación legal de venta según el CAA (puede incluir marcadores como [fruta], [variedad]). */
    @Column(name = "base_name", nullable = false)
    private String baseName;

    /** Descripción ampliada de la denominación y sus requisitos. */
    @Column
    private String description;

    /** Artículo o resolución que funda la denominación. */
    @Column(name = "source_article", nullable = false, length = 300)
    private String sourceArticle;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
