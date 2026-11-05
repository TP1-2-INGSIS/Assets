package com.ingsis.assets.storage;

/**
 * Representa el contenido de un blob recuperado del almacenamiento, junto con su
 * tipo de contenido. Es un tipo agnóstico al proveedor: no conoce Azure ni
 * ningún otro backend concreto.
 *
 * @param content     bytes del blob
 * @param contentType tipo MIME del contenido (puede ser {@code null})
 */
public record StoredBlob(byte[] content, String contentType) {
}
