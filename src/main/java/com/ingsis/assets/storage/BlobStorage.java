package com.ingsis.assets.storage;

import java.util.Optional;

/**
 * Puerto de almacenamiento de blobs, agnóstico al proveedor.
 *
 * <p>El núcleo del servicio (controlador y servicio de dominio) depende
 * únicamente de esta interfaz, nunca de un proveedor concreto. Un módulo
 * independiente (por ejemplo, {@code storage.azure.AzureBlobStorage}) la
 * implementa. Cambiar de proveedor significa añadir otra implementación de esta
 * interfaz, sin tocar el resto del servicio.
 *
 * <p>La interfaz habla de {@code key} y {@code content}; no conoce snippets,
 * UUIDs ni detalles de negocio.
 */
public interface BlobStorage {

  /**
   * Guarda (crea o reemplaza) un blob identificado por {@code key} dentro del
   * {@code container} indicado.
   *
   * @param container   contenedor lógico donde se almacena el blob
   * @param key         clave que identifica el blob dentro del contenedor
   * @param content     bytes a almacenar
   * @param contentType tipo MIME del contenido (puede ser {@code null})
   */
  void put(String container, String key, byte[] content, String contentType);

  /**
   * Recupera el blob identificado por {@code key}.
   *
   * @param container contenedor lógico donde se busca el blob
   * @param key       clave que identifica el blob dentro del contenedor
   * @return el blob si existe, o {@link Optional#empty()} si no
   */
  Optional<StoredBlob> get(String container, String key);

  /**
   * Borra el blob identificado por {@code key}.
   *
   * @param container contenedor lógico donde se busca el blob
   * @param key       clave que identifica el blob dentro del contenedor
   * @return {@code true} si existía y fue borrado; {@code false} si no existía
   */
  boolean delete(String container, String key);
}
