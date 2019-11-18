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
import com.connexta.search.common.Index.IndexBuilder;
import com.connexta.search.common.IndexRepository;
import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.index.exceptions.ContentException;
import com.connexta.search.rest.models.IndexRequest;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@AllArgsConstructor
public class IndexServiceImpl implements IndexService {

  @NotNull private final IndexRepository indexRepository;
  @NotNull private final IonResourceLoader ionResourceLoader;
  private final @NotNull ContentExtractor contentExtractor;

  @Override
  public void index(String datasetId, IndexRequest indexRequest) {
    // TODO check that the dataset exists in S3
    // TODO 11/4/2019 PeterHuffer: this check should be done by the database so separate
    // index instances don't have timing issues
    validateUniqueness(datasetId);
    IndexBuilder builder =
        Index.builder()
            .id(datasetId)
            .fileUrl(indexRequest.getFileLocation())
            .irmUrl(indexRequest.getIrmLocation());
    populateFromIrm(indexRequest.getIrmLocation(), builder);
    builder.contents(extractTextFromFile(indexRequest.getFileLocation()));
    save(builder.build());
  }

  private void validateUniqueness(String datasetId) {
    final boolean idAlreadyExists;
    try {
      idAlreadyExists = indexRepository.existsById(datasetId);
    } catch (final Exception e) {
      throw new SearchException(INTERNAL_SERVER_ERROR, "Unable to query index", e);
    }
    if (idAlreadyExists) {
      throw new SearchException(
          BAD_REQUEST, "Dataset already exists. Overwriting is not supported");
    }
  }

  private void populateFromIrm(String location, IndexBuilder builder) {
    // TODO: THIS IS A PLAEHOLDER TO GET THE TESTS WORKING AGAIN.
    try {
      builder.title(ionResourceLoader.getAsString(location));
    } catch (IOException e) {
      throw new SearchException(HttpStatus.BAD_REQUEST, "Could not read IRM body", e);
    }
  }

  private void save(Index index) {
    log.info("Attempting to index datasetId={}", index.getId());
    try {
      indexRepository.save(index);
    } catch (final Exception e) {
      throw new SearchException(INTERNAL_SERVER_ERROR, "Unable to save index", e);
    }
    log.info("Successfully indexed datasetId={}", index.getId());
  }

  /* TODO: This works up to a certain size. If a file is very large, loading it all into memory as a string will cripple the app */
  private String extractTextFromFile(String location) {
    try (InputStream fileInputStream = ionResourceLoader.get(location)) {
      return contentExtractor.extractText(fileInputStream);
    } catch (ContentException | IOException e) {
      throw new SearchException(
          INTERNAL_SERVER_ERROR,
          String.format("Unable to extract content from file %s", location),
          e);
    }
  }
}
