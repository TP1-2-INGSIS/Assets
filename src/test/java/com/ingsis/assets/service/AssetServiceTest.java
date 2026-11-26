package com.ingsis.assets.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ingsis.assets.persistence.AssetRecord;
import com.ingsis.assets.persistence.AssetRepository;
import com.ingsis.assets.storage.StoredBlob;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Service tests against H2 and the in-memory blob storage (test profile). */
@SpringBootTest
@ActiveProfiles("test")
class AssetServiceTest {

  @Autowired private AssetService service;
  @Autowired private AssetRepository repository;

  @Test
  void saveCreatesThenReplaces() {
    UUID id = UUID.randomUUID();

    assertTrue(service.save(id, "one".getBytes(), "text/plain"));
    assertFalse(service.save(id, "second".getBytes(), "text/x-ps"));

    AssetRecord record = repository.findById(id).orElseThrow();
    assertEquals("snippets", record.getContainer());
    assertEquals(id.toString(), record.getBlobName());
    assertEquals("text/x-ps", record.getContentType());
    assertEquals(6, record.getSize());
    assertNotNull(record.getCreatedAt());
    assertNotNull(record.getUpdatedAt());
    assertEquals(id, record.getSnippetId());
  }

  @Test
  void findReturnsStoredContent() {
    UUID id = UUID.randomUUID();
    service.save(id, "hello".getBytes(), "text/plain");

    StoredBlob blob = service.find(id).orElseThrow();

    assertArrayEquals("hello".getBytes(), blob.content());
    assertEquals("text/plain", blob.contentType());
  }

  @Test
  void findReturnsEmptyWhenUnknown() {
    assertEquals(Optional.empty(), service.find(UUID.randomUUID()));
  }

  @Test
  void deleteRemovesBlobAndRecord() {
    UUID id = UUID.randomUUID();
    service.save(id, "x".getBytes(), null);

    assertTrue(service.delete(id));

    assertTrue(repository.findById(id).isEmpty());
    assertTrue(service.find(id).isEmpty());
  }

  @Test
  void deleteReturnsFalseWhenUnknown() {
    assertFalse(service.delete(UUID.randomUUID()));
  }
}
