# `AssetRecord`

Paquete: `com.ingsis.assets.persistence`

## Qué es

La entidad JPA que Assets mantiene en su propia base de datos para localizar el
blob de un snippet. Es el **índice**, no el contenido.

## Responsabilidad

Guardar, por cada snippet, lo mínimo para encontrar su blob y algo de metadata:

| Campo         | Columna        | Para qué                                     |
|---------------|----------------|----------------------------------------------|
| `snippetId`   | `snippet_id`   | Clave primaria (UUID). La provee el llamador.|
| `container`   | `container`    | Contenedor donde vive el blob.               |
| `blobName`    | `blob_name`    | Nombre del blob dentro del contenedor.       |
| `contentType` | `content_type` | Tipo MIME del contenido (opcional).          |
| `size`        | `size`         | Tamaño en bytes.                             |
| `createdAt`   | `created_at`   | Marca de creación.                           |
| `updatedAt`   | `updated_at`   | Marca de última actualización.               |

Tabla: `asset_records`.

## Detalles

- **No almacena el contenido del snippet**; eso vive en el blob storage. Aquí solo
  queda el puntero y la metadata.
- `snippetId` es la clave primaria, reforzando que Assets es dueño de su índice y
  que las operaciones se direccionan por el `snippetId` que envía Snippets.
- El método `update(contentType, size)` refresca la metadata y `updatedAt` cuando
  se reemplaza el contenido, sin crear un registro nuevo.
- Tiene un constructor protegido sin argumentos requerido por JPA.

## Usado por

- `persistence.AssetRepository`
- `service.AssetService`
