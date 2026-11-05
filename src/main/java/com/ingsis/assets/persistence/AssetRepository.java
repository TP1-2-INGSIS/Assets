package com.ingsis.assets.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA de los registros de assets, indexado por {@code snippetId}.
 */
public interface AssetRepository extends JpaRepository<AssetRecord, UUID> {
}
