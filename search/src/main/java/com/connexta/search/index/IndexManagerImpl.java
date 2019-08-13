/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import com.connexta.search.common.Index;
import com.connexta.search.index.exceptions.IndexException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.repository.CrudRepository;

@Slf4j
public class IndexManagerImpl implements IndexManager {

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
    // TODO verify Media Type for CST
    // TODO check that the product exists in S3

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
      index = new Index(productId, IOUtils.toString(inputStream, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new IndexException("Unable to convert InputStream to String", e);
    }

    log.info("Attempting to index product id {}", productId);
    try {
      crudRepository.save(index);
    } catch (final Exception e) {
      throw new IndexException("Unable to save index", e);
    }
  }
}
