# Comunicación entre servicios (Snippets ↔ Assets ↔ Language)

Documento de **sugerencias**: no es una decisión cerrada, sino las opciones con
sus ventajas e inconvenientes para elegir con criterio.

## Mapa de servicios

| Servicio     | Responsabilidad                                            | Relación con Assets                |
|--------------|------------------------------------------------------------|------------------------------------|
| **Snippets** | Gestiona el dominio de negocio del snippet (autor, permisos, nombre, lenguaje). | **Escribe**: sube el contenido y guarda el ID. |
| **Assets**   | Almacena el contenido como blob y devuelve un ID.          | Es el almacén.                     |
| **Language** | Formatea, lintea y ejecuta el código del snippet.          | **Lee**: baja el contenido por ID. |

Idea clave: **Snippets es el dueño del ID del asset.** Language no inventa IDs;
los recibe (de Snippets) y los usa para leer de Assets.

```
Usuario
  │
  ▼
┌──────────┐  1. POST contenido        ┌──────────┐
│ Snippets │ ────────────────────────▶ │  Assets  │ ──▶ Azure Blob
│          │ ◀──────────────────────── │          │
└──────────┘     { id }                 └──────────┘
  │  guarda id en su DB                      ▲
  │                                          │ 3. GET /v1/assets/{id}
  │  2. entrega id a Language                │
  ▼                                          │
┌──────────┐                                 │
│ Language │ ────────────────────────────────┘
└──────────┘    lee contenido para procesar
```

---

## Opción recomendada: REST síncrono entre servicios

Para este caso (operaciones puntuales de leer/escribir contenido, baja latencia
esperada), **HTTP/REST síncrono** es lo más simple y suficiente.

### Contrato que expone Assets

```
POST   /v1/assets          → body: contenido      → 201 { "id": "<uuid>" }
GET    /v1/assets/{id}      →                       → 200 <contenido crudo>
PUT    /v1/assets/{id}      → body: contenido       → 200
DELETE /v1/assets/{id}      →                       → 204
```

### Flujo de creación (Snippets escribe)

1. Snippets recibe el snippet del usuario.
2. `POST /v1/assets` con el contenido → `201 { "id": "abc-123" }`.
3. Snippets persiste `abc-123` en su propia tabla, asociado a la entidad de
   negocio.

### Flujo de lectura (Language lee)

1. Language necesita procesar el snippet `X`.
2. Pregunta a Snippets por el `assetId` de `X` (o lo recibe en el mensaje que lo
   dispara).
3. `GET /v1/assets/abc-123` → recibe el contenido crudo.
4. Lo formatea/lintea/ejecuta.

### Cómo llamarse entre servicios

- En local (Docker Compose): por **nombre de servicio** en la red interna, p. ej.
  `http://assets:8080/v1/assets/abc-123`.
- En Kubernetes: por el **Service DNS**, p. ej. `http://assets.default.svc.cluster.local`.
- En Spring, usa `RestClient` (Spring 6+/Boot 3+, también en Boot 4) o un
  cliente declarativo con `@HttpExchange` + interface proxy.

Ejemplo de configuración de URL base por entorno (`application.properties`):

```properties
assets.base-url=${ASSETS_BASE_URL:http://localhost:8080}
```

---

## Autenticación entre servicios

Los servicios no deberían confiar ciegamente entre sí si están expuestos.
Opciones, de simple a robusto:

1. **Red privada + API key compartida** (header `X-Api-Key`): simple, suficiente
   si los servicios no son accesibles desde fuera del clúster.
2. **JWT/OAuth2 client-credentials**: cada servicio obtiene un token de un
   emisor central y lo manda en `Authorization: Bearer`. Más estándar y auditable.
3. **mTLS** (si la infraestructura lo soporta): autenticación mutua a nivel de
   transporte.

Para empezar, red privada interna + API key es un punto de partida razonable.

---

## Alternativa: mensajería asíncrona (para procesamiento)

El almacenamiento (Snippets → Assets) encaja bien como REST síncrono. Pero el
**procesamiento** (Language formatea/lintea/ejecuta) puede ser lento y encaja
mejor de forma asíncrona.

Patrón sugerido:

1. Snippets sube el contenido a Assets (REST, como arriba).
2. Snippets publica un evento `SnippetCreated { snippetId, assetId }` en una cola
   (RabbitMQ / Azure Service Bus / Redis Streams).
3. Language **consume** el evento, hace `GET /v1/assets/{assetId}`, procesa, y
   publica el resultado (`SnippetLinted`, `SnippetFormatted`…).

Ventajas: desacopla a Language de los picos de carga, permite reintentos y no
bloquea a Snippets mientras se procesa.

```
Snippets ──REST──▶ Assets
   │
   └──evento SnippetCreated{snippetId, assetId}──▶ [cola] ──▶ Language
                                                                 │
                                                     GET /v1/assets/{assetId}
                                                                 │
                                                                 ▼
                                                              Assets
```

---

## Recomendación final

- **Almacenamiento (Snippets ↔ Assets ↔ Language):** REST síncrono. Simple,
  claro, suficiente.
- **Disparo de procesamiento (Snippets → Language):** empezar con REST; migrar a
  mensajería asíncrona cuando el procesamiento crezca o se necesiten reintentos.
- **Dueño del ID:** Snippets. Assets solo entrega IDs; nunca orquesta.
- **Autenticación:** red interna + API key para empezar, JWT client-credentials
  si los servicios se exponen.
- **Descubrimiento:** nombres de servicio en Compose/K8s, URL base por variable
  de entorno. Nunca hardcodear hosts.
