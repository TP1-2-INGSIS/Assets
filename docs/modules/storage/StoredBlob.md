# `StoredBlob`

Paquete: `com.ingsis.assets.storage`

## Qué es

Un `record` inmutable que representa el contenido de un blob recuperado del
almacenamiento, junto con su tipo de contenido.

```java
public record StoredBlob(byte[] content, String contentType) {}
```

## Responsabilidad

Ser el tipo de retorno de `BlobStorage.get(...)`. Agrupa los dos datos que el
servicio necesita para responder una descarga:

- `content`: los bytes del blob.
- `contentType`: el tipo MIME, que puede ser `null` si el backend no lo guardó.

## Por qué existe

Es parte del puerto agnóstico: al igual que `BlobStorage`, no conoce Azure. Evita
que el núcleo tenga que manipular tipos propios del SDK de un proveedor para
transportar contenido + metadata mínima.

## Usado por

- `storage.BlobStorage` (tipo de retorno de `get`)
- `service.AssetService`
- `controller.AssetController` (para construir la respuesta HTTP)
