# `AssetController`

Paquete: `com.ingsis.assets.controller`

## Qué es

El punto de entrada HTTP del servicio. Expone la API REST de Assets bajo la ruta
base `/v1/assets` y traduce peticiones HTTP en llamadas a `AssetService`.

## Responsabilidad

- Mapear las 4 operaciones del contrato a métodos del servicio.
- Leer el `snippetId` (UUID) de la ruta y el contenido del cuerpo de la petición.
- Traducir el resultado del servicio en el código HTTP correcto.

No contiene lógica de negocio ni toca el almacenamiento directamente: delega todo
en `AssetService`.

## Endpoints

| Método   | Ruta                     | Cuerpo      | Respuesta                        |
|----------|--------------------------|-------------|----------------------------------|
| `POST`   | `/v1/assets/{snippetId}` | contenido   | `201 Created`                    |
| `PUT`    | `/v1/assets/{snippetId}` | contenido   | `200 OK`                         |
| `GET`    | `/v1/assets/{snippetId}` | —           | `200` + contenido, o `404`       |
| `DELETE` | `/v1/assets/{snippetId}` | —           | `204 No Content`, o `404`        |

## Detalles

- El `snippetId` llega como `@PathVariable UUID`; Spring valida el formato UUID.
- El cuerpo se recibe como `byte[]`, de modo que admite tanto `text/plain` como
  binario.
- En `GET`, el `Content-Type` de respuesta se reconstruye desde la metadata del
  blob; si no hay, usa `application/octet-stream`.

## Depende de

- `service.AssetService`
