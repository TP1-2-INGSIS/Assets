package com.ingsis.assets.service;

import com.ingsis.assets.persistence.AssetRecord;
import com.ingsis.assets.persistence.AssetRepository;
import com.ingsis.assets.storage.BlobStorage;
import com.ingsis.assets.storage.StoredBlob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de dominio de Assets.
 *
 * <p>Orquesta el almacenamiento del contenido (a través del puerto
 * {@link BlobStorage}, agnóstico al proveedor) y el registro del
 * {@code snippetId} en la DB propia de Assets. No conoce Azure: solo la interfaz
 * {@link BlobStorage}.
 */
@Service
public class AssetService {

  private final BlobStorage blobStorage;
  private final AssetRepository repository;
  private final String container;

  public AssetService(BlobStorage blobStorage, AssetRepository repository,
      @Value("${azure.storage.container}") String container) {
    this.blobStorage = blobStorage;
    this.repository = repository;
    this.container = container;
  }

  /**
   * Guarda (crea o reemplaza) el contenido asociado a un {@code snippetId}.
   *
   * @return {@code true} si creó un registro nuevo; {@code false} si reemplazó
   *     uno existente
   */
  @Transactional
  public boolean save(UUID snippetId, byte[] content, String contentType) {
    String blobName = snippetId.toString();
    blobStorage.put(container, blobName, content, contentType);

    Optional<AssetRecord> existing = repository.findById(snippetId);
    if (existing.isPresent()) {
      existing.get().update(contentType, content.length);
      return false;
    }
    repository.save(new AssetRecord(snippetId, container, blobName, contentType, content.length));
    return true;
  }

  /** Recupera el contenido asociado a un {@code snippetId}, si existe. */
  @Transactional(readOnly = true)
  public Optional<StoredBlob> find(UUID snippetId) {
    Optional<AssetRecord> record = repository.findById(snippetId);
    if (record.isEmpty()) {
      return Optional.empty();
    }
    return blobStorage.get(record.get().getContainer(), record.get().getBlobName());
  }

  /**
   * Borra el blob y su registro en DB.
   *
   * @return {@code true} si existía y fue borrado; {@code false} si no existía
   */
  @Transactional
  public boolean delete(UUID snippetId) {
    Optional<AssetRecord> record = repository.findById(snippetId);
    if (record.isEmpty()) {
      return false;
    }
    blobStorage.delete(record.get().getContainer(), record.get().getBlobName());
    repository.deleteById(snippetId);
    return true;
  }
}
