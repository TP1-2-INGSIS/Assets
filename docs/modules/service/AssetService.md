# `AssetService`

Paquete: `com.ingsis.assets.service`

## Qué es

El corazón de la lógica de dominio de Assets. Coordina las dos mitades del
almacenamiento: el **contenido** (en el blob storage) y el **índice** (el
`snippetId` en la DB propia de Assets).

## Responsabilidad

- `save(snippetId, content, contentType)`: sube el contenido al blob storage y
  crea o actualiza el registro en DB. Devuelve si fue creación o reemplazo.
- `find(snippetId)`: busca el registro en DB y, si existe, recupera el contenido
  del blob storage.
- `delete(snippetId)`: borra el blob y su registro en DB.

## Por qué es agnóstico al proveedor

Depende de la interfaz `storage.BlobStorage`, **no** de Azure. Spring le inyecta
la implementación concreta en tiempo de ejecución (Azure en producción, una
in-memory en tests). Este es el punto donde se materializa la inversión de
dependencias del servicio.

## Decisiones de diseño

- El nombre del blob se deriva del `snippetId` (`blobName = snippetId.toString()`).
  El mapeo es 1:1, pero la columna `blob_name` existe en la entidad, así que se
  puede desacoplar en el futuro cambiando solo este método.
- El `container` se inyecta desde la propiedad `azure.storage.container`.
- Los métodos son transaccionales (`@Transactional`); las lecturas, de solo
  lectura.

## Depende de

- `storage.BlobStorage` (interfaz)
- `persistence.AssetRepository`
- Propiedad `azure.storage.container`
