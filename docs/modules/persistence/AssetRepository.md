# `AssetRepository`

Paquete: `com.ingsis.assets.persistence`

## Qué es

El repositorio Spring Data JPA de los registros de assets. Da acceso CRUD a la
tabla `asset_records` sin escribir SQL.

```java
public interface AssetRepository extends JpaRepository<AssetRecord, UUID> {}
```

## Responsabilidad

Proveer las operaciones de persistencia que usa `AssetService`:

- `findById(UUID)` — localizar el registro de un `snippetId`.
- `save(AssetRecord)` — crear el registro de un snippet nuevo.
- `deleteById(UUID)` — borrar el registro al eliminar un asset.

El tipo de la clave primaria es `UUID`, coherente con `AssetRecord.snippetId`.

## Por qué así

Al heredar de `JpaRepository`, Spring Data genera la implementación en tiempo de
ejecución. No hace falta código adicional: la interfaz basta para las operaciones
por clave primaria que necesita el servicio.

## Usado por

- `service.AssetService`

## Opera sobre

- `persistence.AssetRecord`
