package com.ingsis.assets.storage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementación de {@link BlobStorage} en memoria para tests.
 *
 * <p>Demuestra que el servicio es agnóstico al proveedor: sustituye por completo
 * a Azure sin tocar el controlador ni el servicio de dominio.
 */
@Component
@Profile("test")
public class InMemoryBlobStorage implements BlobStorage {

  private final Map<String, StoredBlob> store = new ConcurrentHashMap<>();

  private String fullKey(String container, String key) {
    return container + "/" + key;
  }

  @Override
  public void put(String container, String key, byte[] content, String contentType) {
    store.put(fullKey(container, key), new StoredBlob(content.clone(), contentType));
  }

  @Override
  public Optional<StoredBlob> get(String container, String key) {
    return Optional.ofNullable(store.get(fullKey(container, key)));
  }

  @Override
  public boolean delete(String container, String key) {
    return store.remove(fullKey(container, key)) != null;
  }
}
