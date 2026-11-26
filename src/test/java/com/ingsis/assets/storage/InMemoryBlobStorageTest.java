package com.ingsis.assets.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryBlobStorageTest {

  private final InMemoryBlobStorage storage = new InMemoryBlobStorage();

  @Test
  void putGetDelete() {
    byte[] data = {1, 2, 3};
    storage.put("c", "k", data, "text/plain");
    data[0] = 9;

    StoredBlob blob = storage.get("c", "k").orElseThrow();
    assertArrayEquals(new byte[] {1, 2, 3}, blob.content());
    assertEquals("text/plain", blob.contentType());

    assertTrue(storage.delete("c", "k"));
    assertFalse(storage.delete("c", "k"));
    assertTrue(storage.get("c", "k").isEmpty());
  }
}
