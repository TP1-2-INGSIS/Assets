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

- `id` (UUID) — clave que exponemos al resto de servicios
- `container` / `blobName` — ubicación real dentro de Azure Blob
- `contentType`, `size`, `createdAt`, `updatedAt` — metadata opcional

> Nota: para un servicio de almacenamiento puro, la DB es opcional. Si el `id`
> coincide 1:1 con el nombre del blob, se puede prescindir de la DB y usar el
> propio contenedor como índice. Mantener la DB aporta metadata, auditoría y
> desacopla el esquema de nombres de blobs del ID público.

## API propuesta

Un contrato REST mínimo, centrado en operaciones por ID:

| Método | Ruta                 | Descripción                                   |
|--------|----------------------|-----------------------------------------------|
| `POST` | `/v1/assets`         | Sube un blob. Devuelve `{ "id": "<uuid>" }`.  |
| `GET`  | `/v1/assets/{id}`    | Descarga el contenido del blob por ID.        |
| `PUT`  | `/v1/assets/{id}`    | Reemplaza el contenido de un blob existente.  |
| `DELETE`| `/v1/assets/{id}`   | Borra el blob y su referencia en DB.          |

- El cuerpo puede ser `text/plain` (el código del snippet) o binario.
- La respuesta de `POST` devuelve **solo el ID**; el llamador (Snippets) guarda
  ese ID en su propio dominio para asociarlo a su entidad de negocio.

## Flujo de ejemplo

1. **Snippets** recibe un snippet nuevo del usuario.
2. Snippets hace `POST /v1/assets` con el contenido → Assets lo guarda como blob
   y responde `{ "id": "abc-123" }`.
3. Snippets guarda `abc-123` en su propia tabla, junto al resto de datos de
   negocio (autor, nombre, lenguaje, permisos…).
4. Más tarde, **Language** necesita el código para lintearlo: pide a Snippets
   (o directamente) el `id`, y hace `GET /v1/assets/abc-123` → recibe el
   contenido crudo.
5. Language procesa el contenido; si genera una versión formateada, puede
   hacer `PUT /v1/assets/abc-123` o crear un nuevo asset.

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
