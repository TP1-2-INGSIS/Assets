package com.ingsis.assets.storage.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobProperties;
import com.ingsis.assets.storage.BlobStorage;
import com.ingsis.assets.storage.StoredBlob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementación de {@link BlobStorage} sobre Azure Blob Storage.
 *
 * <p>Este es el único punto del servicio que conoce el SDK de Azure. El resto
 * del código depende exclusivamente de la interfaz {@link BlobStorage}.
 */
@Component
@Profile("!test")
public class AzureBlobStorage implements BlobStorage {

  private final BlobServiceClient serviceClient;

  public AzureBlobStorage(BlobServiceClient serviceClient) {
    this.serviceClient = serviceClient;
  }

  @Override
  public void put(String container, String key, byte[] content, String contentType) {
    BlobClient blobClient = containerClient(container).getBlobClient(key);
    blobClient.upload(new ByteArrayInputStream(content), content.length, true);
    if (contentType != null) {
      blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType(contentType));
    }
  }

  @Override
  public Optional<StoredBlob> get(String container, String key) {
    BlobClient blobClient = containerClient(container).getBlobClient(key);
    if (!blobClient.exists()) {
      return Optional.empty();
    }
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    blobClient.downloadStream(buffer);
    BlobProperties properties = blobClient.getProperties();
    return Optional.of(new StoredBlob(buffer.toByteArray(), properties.getContentType()));
  }

  @Override
  public boolean delete(String container, String key) {
    BlobClient blobClient = containerClient(container).getBlobClient(key);
    return blobClient.deleteIfExists();
  }

  private BlobContainerClient containerClient(String container) {
    BlobContainerClient containerClient = serviceClient.getBlobContainerClient(container);
    if (!containerClient.exists()) {
      containerClient.create();
    }
    return containerClient;
  }
}
