# Assets — Comprensión de la funcionalidad

## Responsabilidad

**Assets** es un servicio de almacenamiento de objetos (blobs). Su única
responsabilidad es **persistir contenido arbitrario como blobs** y devolver una
**referencia (ID)** para recuperarlo más tarde.

Es un servicio deliberadamente "tonto": no sabe qué es un snippet, ni su
lenguaje, ni su sintaxis. Solo almacena bytes y entrega referencias. Toda la
lógica de dominio (formatear, lintear, ejecutar) vive en otros servicios.

```
+-----------+        guardar/leer snippet         +-----------+
| Snippets  | ----------------------------------> |           |
+-----------+                                      |           |
                                                   |  Assets   | --> Azure Blob
+-----------+        leer snippet para procesar    |           |     (Azurite local)
| Language  | ----------------------------------> |           |
+-----------+                                      +-----------+
                                                         |
                                                         v
                                                   +-----------+
                                                   |    DB     |  (solo IDs/metadata)
                                                   +-----------+
```

## Qué guarda y dónde

| Dato                    | Dónde se guarda          | Por qué                               |
|-------------------------|--------------------------|---------------------------------------|
| Contenido del snippet   | Azure Blob Storage       | Es contenido grande/variable (bytes). |
| ID del blob + metadata  | Base de datos relacional | Índice ligero para localizar el blob. |

La DB **no** guarda el contenido del snippet, solo lo necesario para
localizar el blob:

- `snippetId` (UUID) — identificador del snippet que Assets recibe en cada
  petición y persiste en su propia tabla. Es la clave por la que Assets localiza
  el blob.
- `container` / `blobName` — ubicación real dentro de Azure Blob
- `contentType`, `size`, `createdAt`, `updatedAt` — metadata opcional

> **Dueño del almacenamiento: Assets.** Snippets no gestiona el esquema de
> blobs ni la DB de Assets; solo hace pedidos HTTP pasando el `snippetId`.
> Assets es quien guarda los `snippetId` en su propia base de datos y resuelve,
> a partir de ellos, dónde está el blob.

## API propuesta

Un contrato REST mínimo, centrado en operaciones por `snippetId` (UUID):

| Método   | Ruta                       | Descripción                                        |
|----------|----------------------------|----------------------------------------------------|
| `POST`   | `/v1/assets/{snippetId}`   | Sube/crea el blob para ese `snippetId`.            |
| `GET`    | `/v1/assets/{snippetId}`   | Descarga el contenido del blob por `snippetId`.    |
| `PUT`    | `/v1/assets/{snippetId}`   | Reemplaza el contenido de un blob existente.       |
| `DELETE` | `/v1/assets/{snippetId}`   | Borra el blob y su referencia en DB.               |

- El cuerpo puede ser `text/plain` (el código del snippet) o binario.
- El `snippetId` lo provee el llamador (Snippets) en la ruta; Assets lo
  **persiste en su propia DB** y lo usa para localizar el blob. Snippets no
  necesita conocer la ubicación interna del blob: solo el `snippetId`.

## Flujo de ejemplo

1. **Snippets** recibe un snippet nuevo del usuario y le asigna un `snippetId`
   (UUID).
2. Snippets hace `POST /v1/assets/{snippetId}` con el contenido → Assets guarda
   el blob y **registra el `snippetId` en su propia DB**.
3. Snippets mantiene su entidad de negocio (autor, nombre, lenguaje, permisos…);
   no necesita guardar ninguna ubicación de blob, solo el `snippetId` que ya
   conoce.
4. Más tarde, **Language** necesita el código para lintearlo: obtiene el
   `snippetId` y hace `GET /v1/assets/{snippetId}` → recibe el contenido crudo.
5. Language procesa el contenido; si genera una versión formateada, puede
   hacer `PUT /v1/assets/{snippetId}`.

## Límites de responsabilidad (lo que Assets NO hace)

- No valida la sintaxis del snippet.
- No conoce lenguajes, usuarios ni permisos de negocio.
- No formatea, lintea ni ejecuta nada.
- No orquesta a otros servicios; solo responde peticiones de almacenamiento.

## Stack actual

- Spring Boot 4.0.0 / Java 21
- Puerto `8080`
- Azure Blob Storage en producción; **Azurite** como emulador local
- Base de datos relacional (p. ej. PostgreSQL) para IDs/metadata

Las dependencias de Azure Blob y de la DB todavía **no** están en `build.gradle`;
se añadirán cuando se implemente la capa de persistencia (ver
`02-azure-blob-setup.md`).
