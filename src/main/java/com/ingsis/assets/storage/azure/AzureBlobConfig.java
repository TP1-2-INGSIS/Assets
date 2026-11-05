package com.ingsis.assets.storage.azure;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuración del cliente de Azure Blob Storage.
 *
 * <p>Construye el {@link BlobServiceClient} a partir de la connection string.
 * Es el único punto, junto con {@link AzureBlobStorage}, que depende del SDK de
 * Azure.
 */
@Configuration
@Profile("!test")
public class AzureBlobConfig {

  @Bean
  public BlobServiceClient blobServiceClient(
      @Value("${azure.storage.connection-string}") String connectionString) {
    return new BlobServiceClientBuilder()
        .connectionString(connectionString)
        .buildClient();
  }
}
