# `AzureBlobConfig`

Paquete: `com.ingsis.assets.storage.azure`

## Qué es

Clase de configuración de Spring (`@Configuration`) que construye el cliente del
SDK de Azure y lo expone como bean.

## Responsabilidad

Crear un `BlobServiceClient` a partir de la connection string:

```java
@Bean
BlobServiceClient blobServiceClient(
    @Value("${azure.storage.connection-string}") String connectionString) {
  return new BlobServiceClientBuilder()
      .connectionString(connectionString)
      .buildClient();
}
```

## Por qué está separada de `AzureBlobStorage`

- Aísla el **cómo se construye** el cliente (credenciales/endpoint) del **cómo se
  usa** (las operaciones de blob). Junto con `AzureBlobStorage`, es el único lugar
  que depende del SDK de Azure.
- La connection string se resuelve desde `azure.storage.connection-string`, que
  en local apunta a Azurite y en producción se inyecta por variable de entorno o
  secret manager.

## Activación por perfil

Anotada `@Profile("!test")`: no se carga en tests, donde no hay un backend Azure
real al que conectarse.

## Depende de

- `com.azure.storage.blob.BlobServiceClient` / `BlobServiceClientBuilder`
- Propiedad `azure.storage.connection-string`
