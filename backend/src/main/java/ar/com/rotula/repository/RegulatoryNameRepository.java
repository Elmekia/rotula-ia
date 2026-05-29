package ar.com.rotula.repository;

import ar.com.rotula.domain.RegulatoryName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegulatoryNameRepository extends JpaRepository<RegulatoryName, UUID> {

    /** Busca la denominación legal activa para una categoría (búsqueda exacta, case-insensitive). */
    Optional<RegulatoryName> findByCategoryIgnoreCaseAndActiveTrue(String category);
}
