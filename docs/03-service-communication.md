# Comunicación entre servicios (Snippets ↔ Assets ↔ Language)

Documento de **sugerencias**: no es una decisión cerrada, sino las opciones con
sus ventajas e inconvenientes para elegir con criterio.

## Mapa de servicios

| Servicio     | Responsabilidad                                            | Relación con Assets                |
|--------------|------------------------------------------------------------|------------------------------------|
| **Snippets** | Gestiona el dominio de negocio del snippet (autor, permisos, nombre, lenguaje). | **Escribe**: envía `snippetId` + contenido. |
| **Assets**   | Almacena el contenido como blob y registra el `snippetId` en su propia DB. | Es el almacén y dueño del índice.  |
| **Language** | Formatea, lintea y ejecuta el código del snippet.          | **Lee**: baja el contenido por `snippetId`. |

Idea clave: **Assets es el dueño de su propio índice.** Snippets solo hace
pedidos pasando el `snippetId`; no conoce ni gestiona el esquema de blobs ni la
DB de Assets. Language tampoco inventa IDs: usa el `snippetId` para leer de
Assets.

```
Usuario
  │
  ▼
┌──────────┐  1. POST /v1/assets/{snippetId}   ┌──────────┐
│ Snippets │ ────────────────────────────────▶ │  Assets  │ ──▶ Azure Blob
│          │ ◀──────────────────────────────── │          │ ──▶ DB (snippetId)
└──────────┘            201                      └──────────┘
  │                                                   ▲
  │  2. entrega snippetId a Language                  │ 3. GET /v1/assets/{snippetId}
  ▼                                                   │
┌──────────┐                                          │
│ Language │ ──────────────────────────────────────── ┘
└──────────┘    lee contenido para procesar
```

---

## Opción recomendada: REST síncrono entre servicios

Para este caso (operaciones puntuales de leer/escribir contenido, baja latencia
esperada), **HTTP/REST síncrono** es lo más simple y suficiente.

### Contrato que expone Assets

```
POST   /v1/assets/{snippetId}   → body: contenido   → 201
GET    /v1/assets/{snippetId}   →                     → 200 <contenido crudo>
PUT    /v1/assets/{snippetId}   → body: contenido     → 200
DELETE /v1/assets/{snippetId}   →                     → 204
```

### Flujo de creación (Snippets escribe)

1. Snippets recibe el snippet del usuario y le asigna un `snippetId` (UUID).
2. `POST /v1/assets/{snippetId}` con el contenido → `201`.
3. Assets guarda el blob y registra el `snippetId` en su propia DB. Snippets no
   almacena ninguna ubicación de blob.

### Flujo de lectura (Language lee)

1. Language necesita procesar el snippet `X`.
2. Obtiene el `snippetId` de `X` (de Snippets o del mensaje que lo dispara).
3. `GET /v1/assets/{snippetId}` → recibe el contenido crudo.
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
2. Snippets publica un evento `SnippetCreated { snippetId }` en una cola
   (RabbitMQ / Azure Service Bus / Redis Streams).
3. Language **consume** el evento, hace `GET /v1/assets/{snippetId}`, procesa, y
   publica el resultado (`SnippetLinted`, `SnippetFormatted`…).

Ventajas: desacopla a Language de los picos de carga, permite reintentos y no
bloquea a Snippets mientras se procesa.

```
Snippets ──REST──▶ Assets
   │
   └──evento SnippetCreated{snippetId}──▶ [cola] ──▶ Language
                                                        │
                                            GET /v1/assets/{snippetId}
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
- **Dueño del índice:** Assets. Assets registra los `snippetId` en su propia DB;
  Snippets solo hace pedidos y nunca orquesta el almacenamiento.
- **Autenticación:** red interna + API key para empezar, JWT client-credentials
  si los servicios se exponen.
- **Descubrimiento:** nombres de servicio en Compose/K8s, URL base por variable
  de entorno. Nunca hardcodear hosts.
