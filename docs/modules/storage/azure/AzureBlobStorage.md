# `AzureBlobStorage`

Paquete: `com.ingsis.assets.storage.azure`

## Qué es

El **adaptador** que implementa la interfaz `storage.BlobStorage` usando el SDK
de Azure Blob Storage. Es el único punto del código de producción que importa
clases `com.azure.*`.

## Responsabilidad

Traducir las operaciones genéricas del puerto a llamadas concretas del SDK:

- `put`: sube los bytes con `BlobClient.upload(...)` (sobrescribe si existe) y,
  si hay `contentType`, lo fija vía `setHttpHeaders`.
- `get`: comprueba existencia, descarga el stream a memoria y lee el
  `contentType` desde las propiedades del blob.
- `delete`: usa `deleteIfExists()`, devolviendo si el blob existía.

## Detalles

- Antes de operar, asegura que el `container` exista (`create()` si falta). Esto
  evita el paso manual `az storage container create` de la guía de Azurite.
- Funciona igual contra Azurite (local) y Azure real: solo cambia la connection
  string, que recibe a través del `BlobServiceClient` inyectado.

## Activación por perfil

Anotado `@Profile("!test")`: se registra en todos los perfiles **excepto** en
tests, donde se usa una implementación in-memory. Así el build no necesita
Azurite.

## Depende de

- `com.azure.storage.blob.BlobServiceClient` (provisto por `AzureBlobConfig`)
- Implementa: `storage.BlobStorage`
