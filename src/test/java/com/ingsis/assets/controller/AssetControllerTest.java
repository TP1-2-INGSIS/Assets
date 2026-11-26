package com.ingsis.assets.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ingsis.assets.service.AssetService;
import com.ingsis.assets.storage.StoredBlob;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AssetControllerTest {

  private static final byte[] BODY = "let a = 1;".getBytes();

  @Mock private AssetService service;

  private MockMvc mvc;
  private final UUID id = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(new AssetController(service)).build();
  }

  @Test
  void createReturns201() throws Exception {
    mvc.perform(post("/v1/assets/" + id).contentType(MediaType.TEXT_PLAIN).content(BODY))
        .andExpect(status().isCreated());
    verify(service).save(eq(id), eq(BODY), eq("text/plain"));
  }

  @Test
  void replaceReturns200() throws Exception {
    mvc.perform(put("/v1/assets/" + id).contentType(MediaType.TEXT_PLAIN).content(BODY))
        .andExpect(status().isOk());
    verify(service).save(eq(id), eq(BODY), any());
  }

  @Test
  void getReturnsContentWithStoredContentType() throws Exception {
    when(service.find(id)).thenReturn(Optional.of(new StoredBlob(BODY, "text/plain")));
    mvc.perform(get("/v1/assets/" + id))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().bytes(BODY));
  }

  @Test
  void getDefaultsToOctetStreamWhenNoContentType() throws Exception {
    when(service.find(id)).thenReturn(Optional.of(new StoredBlob(BODY, null)));
    mvc.perform(get("/v1/assets/" + id))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM));
  }

  @Test
  void getReturns404WhenMissing() throws Exception {
    when(service.find(id)).thenReturn(Optional.empty());
    mvc.perform(get("/v1/assets/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void deleteReturns204WhenDeleted() throws Exception {
    when(service.delete(id)).thenReturn(true);
    mvc.perform(delete("/v1/assets/" + id)).andExpect(status().isNoContent());
  }

  @Test
  void deleteReturns404WhenMissing() throws Exception {
    when(service.delete(id)).thenReturn(false);
    mvc.perform(delete("/v1/assets/" + id)).andExpect(status().isNotFound());
  }
}
