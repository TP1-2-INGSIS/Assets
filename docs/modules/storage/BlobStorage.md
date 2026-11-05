# `BlobStorage`

Paquete: `com.ingsis.assets.storage`

## Qué es

El **puerto** de almacenamiento de blobs: una interfaz Java agnóstica al
proveedor. Es el contrato que el núcleo del servicio conoce, y la pieza central
del diseño de inversión de dependencias.

## Responsabilidad

Definir las operaciones mínimas de almacenamiento de objetos, sin revelar cómo ni
dónde se guardan:

```java
void put(String container, String key, byte[] content, String contentType);
Optional<StoredBlob> get(String container, String key);
boolean delete(String container, String key);
```

## Por qué existe

- El `controller` y el `service` dependen **solo** de esta interfaz. No importan
  clases de Azure ni de ningún SDK.
- Cambiar de proveedor (S3, filesystem, GCS…) se reduce a añadir una clase que la
  implemente; el resto del código no cambia.
- Facilita los tests: una implementación in-memory sustituye por completo al
  backend real.

## Vocabulario deliberadamente genérico

La interfaz habla de `container`, `key` y `content`. No conoce "snippets",
UUIDs ni reglas de negocio: esa traducción ocurre en `service.AssetService`.

## Implementaciones

- `storage.azure.AzureBlobStorage` — producción (Azure Blob / Azurite).
- `InMemoryBlobStorage` (en el código de tests) — para pruebas sin infraestructura.
