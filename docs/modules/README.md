# Módulos de Assets

Documentación por módulo del servicio. Cada carpeta refleja un paquete Java de
`com.ingsis.assets`, y cada `.md` describe una clase: su responsabilidad, de qué
depende y por qué existe.

## Mapa de dependencias

```
controller  ──►  service  ──►  storage (interfaz BlobStorage)
                    │                   ▲
                    ▼                   │
               persistence       storage/azure (implementación)
                 (JPA/DB)
```

Idea rectora: **inversión de dependencias**. El núcleo (`controller`, `service`)
depende de la interfaz `storage.BlobStorage`, nunca de un proveedor concreto.
Azure es solo una implementación de esa interfaz, aislada en `storage/azure`.

## Carpetas

| Carpeta                        | Responsabilidad                                          |
|--------------------------------|----------------------------------------------------------|
| [`controller`](controller/)    | Capa REST. Expone `/v1/assets/{snippetId}`.              |
| [`service`](service/)          | Lógica de dominio. Orquesta blob + DB.                   |
| [`storage`](storage/)          | Puerto de almacenamiento, agnóstico al proveedor.        |
| [`storage/azure`](storage/azure/) | Adaptador Azure Blob. Único módulo que conoce el SDK. |
| [`persistence`](persistence/)  | Entidad y repositorio JPA (índice por `snippetId`).      |
