# Configurar Azure Blob Storage desde cero (guía paso a paso)

Esta guía cubre dos entornos:

- **Local (Azurite):** emulador gratuito de Azure Storage, para desarrollo y tests.
- **Azure real (nube):** la cuenta de almacenamiento de producción.

> Terminología: en Azure no existe el término "bucket" (eso es S3). El
> equivalente es un **Storage Account** que contiene uno o más **Containers**,
> y dentro de cada container se guardan los **Blobs**. Mentalmente:
> `Storage Account ≈ cuenta`, `Container ≈ bucket`, `Blob ≈ objeto/archivo`.

---

## Parte A — Desarrollo local con Azurite

Azurite emula la API de Blob Storage en tu máquina. Es lo que usaremos a diario.

### A.1. Levantar Azurite con Docker (recomendado)

Añade este servicio al `docker-compose.yml` del proyecto:

```yaml
services:
  azurite:
    image: mcr.microsoft.com/azure-storage/azurite:latest
    container_name: azurite
    command: "azurite-blob --blobHost 0.0.0.0 --loose"
    ports:
      - "10000:10000"   # Blob service
    volumes:
      - azurite-data:/data

volumes:
  azurite-data:
```

Levántalo:

```bash
docker compose up -d azurite
```

### A.2. Cadena de conexión de Azurite

Azurite usa una **cuenta de desarrollo fija y pública** (son credenciales de
juguete, iguales para todo el mundo, no son secretas):

```
DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
```

> Si Assets corre **dentro** de Docker Compose junto a Azurite, reemplaza
> `127.0.0.1` por `azurite` (el nombre del servicio) en el `BlobEndpoint`.

### A.3. Crear el container

Con el **Azure CLI** apuntando a Azurite:

```bash
az storage container create \
  --name snippets \
  --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;"
```

Alternativa gráfica: **Azure Storage Explorer** → "Attach to a local emulator".

Con esto ya puedes desarrollar sin tocar la nube.

---

## Parte B — Azure real (producción), paso a paso

### Prerrequisitos

- Una cuenta de Azure con una suscripción activa.
- Azure CLI instalado (`az`). Inicia sesión:

```bash
az login
```

### B.1. Crear un Resource Group

Un *resource group* es un contenedor lógico para agrupar recursos relacionados.

```bash
az group create \
  --name rg-ingsis \
  --location eastus
```

### B.2. Crear la Storage Account (el "bucket")

El nombre debe ser **globalmente único**, 3–24 caracteres, solo minúsculas y
números.

```bash
az storage account create \
  --name ingsisassets$RANDOM \
  --resource-group rg-ingsis \
  --location eastus \
  --sku Standard_LRS \
  --kind StorageV2 \
  --min-tls-version TLS1_2 \
  --allow-blob-public-access false
```

- `Standard_LRS`: replicación local, la opción más económica.
- `--allow-blob-public-access false`: los blobs NO son públicos; solo se
  acceden con credenciales. Es lo correcto para contenido privado de usuarios.

Guarda el nombre que elegiste; lo llamaremos `<STORAGE_ACCOUNT>`.

### B.3. Crear el Container

```bash
az storage container create \
  --name snippets \
  --account-name <STORAGE_ACCOUNT> \
  --auth-mode login
```

### B.4. Obtener credenciales

Tienes tres formas de autenticar Assets contra Azure. De menos a más segura:

#### Opción 1 — Connection string (rápida, para empezar)

```bash
az storage account show-connection-string \
  --name <STORAGE_ACCOUNT> \
  --resource-group rg-ingsis \
  --output tsv
```

Esto devuelve una cadena tipo:

```
DefaultEndpointsProtocol=https;AccountName=...;AccountKey=...;EndpointSuffix=core.windows.net
```

La `AccountKey` da acceso TOTAL a la cuenta → trátala como un secreto. Úsala
para prototipar, pero no es la mejor opción para producción.

#### Opción 2 — Managed Identity (recomendada en producción)

Si Assets corre en Azure (App Service, Container Apps, AKS), asígnale una
**identidad administrada** y concédele el rol sobre la cuenta. Así no hay
claves que rotar ni guardar:

```bash
# Rol "Storage Blob Data Contributor" sobre la cuenta
az role assignment create \
  --assignee <PRINCIPAL_ID_DE_LA_IDENTIDAD> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUB_ID>/resourceGroups/rg-ingsis/providers/Microsoft.Storage/storageAccounts/<STORAGE_ACCOUNT>
```

En código, el SDK usa `DefaultAzureCredential`, que detecta la identidad
automáticamente. No se guarda ninguna clave.

#### Opción 3 — SAS token (acceso acotado y temporal)

Para dar acceso limitado (solo lectura, solo a un container, con caducidad):

```bash
az storage container generate-sas \
  --name snippets \
  --account-name <STORAGE_ACCOUNT> \
  --permissions rwl \
  --expiry 2026-12-31 \
  --auth-mode login --as-user
```

### B.5. Resumen de qué configurar

| Recurso           | Nombre de ejemplo        | Para qué sirve                        |
|-------------------|--------------------------|---------------------------------------|
| Resource Group    | `rg-ingsis`              | Agrupa todos los recursos.            |
| Storage Account   | `ingsisassetsXXXX`       | La "cuenta"/bucket raíz.              |
| Container         | `snippets`               | Carpeta lógica donde van los blobs.   |
| Credencial        | connection string / MI   | Cómo autentica Assets.                |

---

## Parte C — Conectar Assets (Spring Boot) al almacenamiento

### C.1. Dependencias (build.gradle)

Opción con el starter de Spring Cloud Azure (gestiona autoconfiguración):

```groovy
dependencies {
    implementation 'com.azure.spring:spring-cloud-azure-starter-storage-blob:5.x.x'
    // ...resto de dependencias
}
```

O el SDK "pelado" de Azure si prefieres control manual:

```groovy
dependencies {
    implementation 'com.azure:azure-storage-blob:12.36.0'
    implementation 'com.azure:azure-identity:1.x.x' // para DefaultAzureCredential
}
```

> Verifica las versiones exactas en Maven Central al implementar; aquí van como
> referencia.

### C.2. Configuración por perfiles

`application.properties` (perfil local, apuntando a Azurite):

```properties
azure.storage.connection-string=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
azure.storage.container=snippets
```

En producción, NO pongas la cadena en el archivo. Usa variables de entorno o un
secret manager (Azure Key Vault). Ejemplo con variable de entorno:

```properties
azure.storage.connection-string=${AZURE_STORAGE_CONNECTION_STRING}
azure.storage.container=${AZURE_STORAGE_CONTAINER:snippets}
```

### C.3. Checklist de puesta en marcha

1. [ ] Azurite levantado (`docker compose up -d azurite`).
2. [ ] Container `snippets` creado en Azurite.
3. [ ] Dependencias de Azure añadidas a `build.gradle`.
4. [ ] `application.properties` con la connection string local.
5. [ ] Para la nube: Resource Group + Storage Account + Container creados.
6. [ ] Credencial de producción resuelta vía Managed Identity o Key Vault.
7. [ ] Las claves reales NUNCA se commitean (ver `.gitignore`).

---

## Seguridad — no commitees secretos

- La connection string **de Azurite** es pública y puede ir en el repo.
- La `AccountKey` o connection string **de producción** es un secreto: debe ir
  en variables de entorno, GitHub Actions secrets o Azure Key Vault, nunca en
  git.
- Preferir Managed Identity elimina el manejo de claves por completo.
