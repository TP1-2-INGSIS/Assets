package com.ingsis.assets.controller;

import com.ingsis.assets.service.AssetService;
import com.ingsis.assets.storage.StoredBlob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de Assets. Todas las operaciones se indexan por {@code snippetId}
 * (UUID), que el llamador (Snippets) provee en la ruta.
 */
@RestController
@RequestMapping("/v1/assets")
public class AssetController {

  private final AssetService service;

  public AssetController(AssetService service) {
    this.service = service;
  }

  /** Crea el blob para un {@code snippetId}. Devuelve 201. */
  @PostMapping("/{snippetId}")
  public ResponseEntity<Void> create(
      @PathVariable UUID snippetId,
      @RequestBody byte[] content,
      @RequestHeader(value = "Content-Type", required = false) String contentType) {
    service.save(snippetId, content, contentType);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /** Reemplaza el contenido de un blob existente. Devuelve 200. */
  @PutMapping("/{snippetId}")
  public ResponseEntity<Void> replace(
      @PathVariable UUID snippetId,
      @RequestBody byte[] content,
      @RequestHeader(value = "Content-Type", required = false) String contentType) {
    service.save(snippetId, content, contentType);
    return ResponseEntity.ok().build();
  }

  /** Descarga el contenido crudo del blob por {@code snippetId}. */
  @GetMapping("/{snippetId}")
  public ResponseEntity<byte[]> get(@PathVariable UUID snippetId) {
    Optional<StoredBlob> blob = service.find(snippetId);
    if (blob.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    String contentType = blob.get().contentType();
    MediaType mediaType = contentType != null
        ? MediaType.parseMediaType(contentType)
        : MediaType.APPLICATION_OCTET_STREAM;
    return ResponseEntity.ok().contentType(mediaType).body(blob.get().content());
  }

  /** Borra el blob y su referencia en DB. Devuelve 204, o 404 si no existe. */
  @DeleteMapping("/{snippetId}")
  public ResponseEntity<Void> delete(@PathVariable UUID snippetId) {
    boolean deleted = service.delete(snippetId);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }
}
