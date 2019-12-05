/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.index.Index.IndexBuilder;
import com.connexta.search.index.exceptions.ContentException;
import com.connexta.search.rest.models.IndexRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class IndexServiceImpl implements IndexService {

  @NotNull private final IndexStorageAdaptor indexStorageAdaptor;
  @NotNull private final IonResourceLoader ionResourceLoader;
  @NotNull private final ContentExtractor contentExtractor;

  @Override
  public void index(final UUID datasetId, final IndexRequest indexRequest) {
    // TODO check that the dataset exists in S3
    // TODO 11/4/2019 PeterHuffer: this check should be done by the database so separate index
    // instances don't have timing issues
    // TODO validate args
    final IndexBuilder builder = Index.builder().id(validateUniqueness(datasetId.toString()));
    populateFromIrm(indexRequest.getIrmLocation(), builder);
    populateFromFile(indexRequest.getFileLocation(), builder);
    populateFromMetacard(indexRequest.getMetacardLocation(), builder);
    save(datasetId, builder.build());
  }

  private String validateUniqueness(String datasetIdString) {
    final boolean idAlreadyExists;
    try {
      idAlreadyExists = indexStorageAdaptor.existsById(datasetIdString);
    } catch (final Exception e) {
      throw new SearchException(INTERNAL_SERVER_ERROR, "Unable to query index", e);
    }
    if (idAlreadyExists) {
      throw new SearchException(
          BAD_REQUEST, "Dataset already exists. Overwriting is not supported");
    }

    return datasetIdString;
  }

  private static void populateFromIrm(String location, IndexBuilder builder) {
    // TODO Index IRM
    builder.irmUrl(location);
  }

  /* TODO: This works up to a certain size. If a file is very large, loading it all into memory as a string will cripple the app */
  private void populateFromFile(String location, IndexBuilder builder) {
    final String extractedText;
    try (InputStream fileInputStream = ionResourceLoader.get(location)) {
      extractedText = contentExtractor.extractText(fileInputStream);
    } catch (ContentException | IOException e) {
      throw new SearchException(
          INTERNAL_SERVER_ERROR,
          String.format("Unable to extract content from file %s", location),
          e);
    }

    builder.contents(extractedText);
    builder.fileUrl(location);
  }

  private void populateFromMetacard(final String location, final IndexBuilder builder) {
    builder.metacardUrl(location);
  }

  private void save(final UUID datasetId, final Index index) {
    log.info("Attempting to index datasetId={}", datasetId);
    try {
      indexStorageAdaptor.save(index);
    } catch (final Exception e) {
      final String message = "Unable to save index";
      log.warn(message, e);
      throw new SearchException(INTERNAL_SERVER_ERROR, message, e);
    }
    log.info("Successfully indexed datasetId={}", datasetId);
  }
}
