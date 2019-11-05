/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import com.connexta.search.index.IndexService;
import com.connexta.search.query.exceptions.QueryException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class QueryServiceImpl implements QueryService {

  private static final String IRM_PATH_SEGMENT = "irm";
  @NotBlank private final String datasetRetrieveEndpoint;
  private final IndexService indexService;

  public QueryServiceImpl(
      @NotBlank final String datasetRetrieveEndpoint, @NotNull IndexService indexService) {
    this.datasetRetrieveEndpoint = datasetRetrieveEndpoint;
    this.indexService = indexService;
  }

  @Override
  public List<URI> find(final String cqlString) {
    return getIrmUris(indexService.query(cqlString));
  }

  /**
   * Creates a {@link List} of IRM retrieve {@link URI}s from the {@code matchingDatasetIds} using
   * the provided {@link #datasetRetrieveEndpoint}
   *
   * @param matchingDatasetIds may be empty
   * @return a set of IRM retrieve {@link URI}s
   * @throws QueryException if unable to construct a retrieve URI
   */
  private List<URI> getIrmUris(final Set<String> matchingDatasetIds) {
    return matchingDatasetIds.stream()
        .map(this::constructIrmUri)
        .collect(Collectors.toUnmodifiableList());
  }

  private URI constructIrmUri(String datasetId) {
    try {
      return UriComponentsBuilder.fromUriString(datasetRetrieveEndpoint)
          .pathSegment(datasetId)
          .pathSegment(IRM_PATH_SEGMENT)
          .build()
          .toUri();
    } catch (IllegalArgumentException e) {
      throw new QueryException(
          String.format(
              "Unable to construct IRM retrieve URI from endpointUrl.datasetRetrieve=%s, datasetId=%s, and irmPathSegment=%s",
              datasetRetrieveEndpoint, datasetId, IRM_PATH_SEGMENT),
          e);
    }
  }
}
