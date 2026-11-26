package com.ingsis.assets.storage.azure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobProperties;
import com.ingsis.assets.storage.StoredBlob;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AzureBlobStorageTest {

  @Mock private BlobServiceClient serviceClient;
  @Mock private BlobContainerClient containerClient;
  @Mock private BlobClient blobClient;

  private AzureBlobStorage storage;

  @BeforeEach
  void setUp() {
    when(serviceClient.getBlobContainerClient("c")).thenReturn(containerClient);
    when(containerClient.getBlobClient("k")).thenReturn(blobClient);
    storage = new AzureBlobStorage(serviceClient);
  }

  @Test
  void putUploadsAndSetsContentTypeCreatingContainer() {
    when(containerClient.exists()).thenReturn(false);

    storage.put("c", "k", new byte[] {1, 2}, "text/plain");

    verify(containerClient).create();
    verify(blobClient).upload(any(InputStream.class), anyLong(), any(Boolean.class));
    verify(blobClient).setHttpHeaders(any(BlobHttpHeaders.class));
  }

  @Test
  void putWithoutContentTypeSkipsHeaders() {
    when(containerClient.exists()).thenReturn(true);

    storage.put("c", "k", new byte[] {1}, null);

    verify(containerClient, never()).create();
    verify(blobClient, never()).setHttpHeaders(any());
  }

  @Test
  void getReturnsEmptyWhenBlobMissing() {
    when(containerClient.exists()).thenReturn(true);
    when(blobClient.exists()).thenReturn(false);

    assertEquals(Optional.empty(), storage.get("c", "k"));
  }

  @Test
  void getDownloadsContentAndType() {
    when(containerClient.exists()).thenReturn(true);
    when(blobClient.exists()).thenReturn(true);
    doAnswer(
            inv -> {
              inv.<OutputStream>getArgument(0).write(new byte[] {7, 8});
              return null;
            })
        .when(blobClient)
        .downloadStream(any(OutputStream.class));
    BlobProperties props = org.mockito.Mockito.mock(BlobProperties.class);
    when(props.getContentType()).thenReturn("text/plain");
    when(blobClient.getProperties()).thenReturn(props);

    StoredBlob blob = storage.get("c", "k").orElseThrow();

    assertArrayEquals(new byte[] {7, 8}, blob.content());
    assertEquals("text/plain", blob.contentType());
  }

  @Test
  void deleteDelegatesToDeleteIfExists() {
    when(containerClient.exists()).thenReturn(true);
    when(blobClient.deleteIfExists()).thenReturn(true, false);

    assertTrue(storage.delete("c", "k"));
    assertFalse(storage.delete("c", "k"));
  }
}
