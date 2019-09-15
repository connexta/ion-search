/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.connexta.search.common.Index;
import com.connexta.search.index.exceptions.IndexException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.data.repository.CrudRepository;

@Slf4j
public class IndexManagerImpl implements IndexManager {

  private static final String EXT_EXTRACTED_TEXT = "ext.extracted.text";
  private final CrudRepository crudRepository;

  public IndexManagerImpl(@NotNull final CrudRepository crudRepository) {
    this.crudRepository = crudRepository;
  }

  @Override
  public void index(
      @NotBlank final String productId,
      @NotBlank final String mediaType,
      @NotNull final InputStream inputStream)
      throws IndexException {
    // TODO check that the product exists in S3

    if (!StringUtils.equals(mediaType, ContentType.APPLICATION_JSON.getMimeType())) {
      throw new IndexException(
          INTERNAL_SERVER_ERROR,
          "Expected the content type to be "
              + ContentType.APPLICATION_JSON.getMimeType()
              + " , but was "
              + mediaType);
    }

    final boolean idAlreadyExists;
    try {
      idAlreadyExists = crudRepository.existsById(productId);
    } catch (final Exception e) {
      throw new IndexException(INTERNAL_SERVER_ERROR, "Unable to query index", e);
    }
    if (idAlreadyExists) {
      throw new IndexException(BAD_REQUEST, "Product already exists. Overwriting is not supported");
    }

    final String contents;
    try {
      contents = getElement(parseJson(inputStream), EXT_EXTRACTED_TEXT);
    } catch (IOException e) {
      throw new IndexException(INTERNAL_SERVER_ERROR, "Unable to convert InputStream to JSON", e);
    }

    log.info("Attempting to index product id {}", productId);
    try {
      crudRepository.save(new Index(productId, contents));
    } catch (final Exception e) {
      throw new IndexException(INTERNAL_SERVER_ERROR, "Unable to save index", e);
    }
  }

  private static JsonNode parseJson(InputStream stream) throws IOException {
    final ObjectMapper objectMapper;
    objectMapper = new ObjectMapper();
    return objectMapper.readTree(stream);
  }

  private static String getElement(JsonNode json, String fieldName) throws IndexException {
    if (json.get(fieldName) == null) {
      throw new IndexException(INTERNAL_SERVER_ERROR, "JSON is malformed");
    }
    return json.get(fieldName).asText();
  }
}
