package ar.com.rotula.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro de auditoría de acciones del sistema.
 * Persiste en la tabla {@code audit_log} (sin RLS — tabla global de auditoría).
 */
@Entity
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    /** Tipo de entidad auditada (LABEL, PRODUCT, etc.). */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** ID de la entidad auditada. */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    /** Acción realizada (EXPORT, CREATE, UPDATE, DELETE). */
    @Column(nullable = false, length = 50)
    private String action;

    /** Detalles adicionales como JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
