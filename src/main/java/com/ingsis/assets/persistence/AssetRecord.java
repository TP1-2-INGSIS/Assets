package com.ingsis.assets.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro que Assets mantiene en su propia DB para localizar el blob de un
 * snippet. La clave es el {@code snippetId} (UUID) que provee el llamador.
 *
 * <p>No guarda el contenido del snippet, solo su ubicación y metadata.
 */
@Entity
@Table(name = "asset_records")
public class AssetRecord {

  @Id
  @Column(name = "snippet_id", nullable = false, updatable = false)
  private UUID snippetId;

  @Column(name = "container", nullable = false)
  private String container;

  @Column(name = "blob_name", nullable = false)
  private String blobName;

  @Column(name = "content_type")
  private String contentType;

  @Column(name = "size")
  private long size;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AssetRecord() {
    // Requerido por JPA.
  }

  public AssetRecord(UUID snippetId, String container, String blobName, String contentType,
      long size) {
    this.snippetId = snippetId;
    this.container = container;
    this.blobName = blobName;
    this.contentType = contentType;
    this.size = size;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  /** Actualiza la metadata del registro tras un reemplazo del contenido. */
  public void update(String contentType, long size) {
    this.contentType = contentType;
    this.size = size;
    this.updatedAt = Instant.now();
  }

  public UUID getSnippetId() {
    return snippetId;
  }

  public String getContainer() {
    return container;
  }

  public String getBlobName() {
    return blobName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSize() {
    return size;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
