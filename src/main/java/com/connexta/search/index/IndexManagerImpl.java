/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import com.connexta.search.common.Index;
import com.connexta.search.index.exceptions.IndexException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class IndexManagerImpl implements IndexManager {

  public static final String EXT_EXTRACTED_TEXT = "ext.extracted.text";
  private final CrudRepository crudRepository;

  public IndexManagerImpl(@NotNull final CrudRepository crudRepository) {
    this.crudRepository = crudRepository;
  }

  @Override
  public void index(
      @NotBlank final String productId,
      @NotBlank final String mediaType,
      @NotNull final MultipartFile file)
      throws IndexException {
    // TODO check that the product exists in S3

    if (!mediaType.equals(MediaType.APPLICATION_JSON_VALUE)) {
      throw new IndexException("The CST file's media type is not application/json");
    }

    final boolean idAlreadyExists;
    try {
      idAlreadyExists = crudRepository.existsById(productId);
    } catch (final Exception e) {
      throw new IndexException("Unable to query index", e);
    }
    if (idAlreadyExists) {
      throw new IndexException("Product already exists. Overriding is not supported");
    }

    final Index index;
    try {
      JsonNode jsonNode = parseJson(file.getInputStream());
      index = new Index(productId, getElement(jsonNode, EXT_EXTRACTED_TEXT));
    } catch (IOException e) {
      throw new IndexException("Unable to convert InputStream to JSON", e);
    }

    log.info("Attempting to index product id {}", productId);
    try {
      crudRepository.save(index);
    } catch (final Exception e) {
      throw new IndexException("Unable to save index", e);
    }
  }

  private JsonNode parseJson(InputStream stream) throws IOException {
    final ObjectMapper objectMapper;
    objectMapper = new ObjectMapper();
    return objectMapper.readTree(stream);
  }

  private String getElement(JsonNode json, String fieldName) {
    return json.get(fieldName).asText();
  }
}
