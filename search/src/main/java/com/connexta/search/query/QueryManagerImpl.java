/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import com.connexta.search.common.Index;
import com.connexta.search.common.IndexCrudRepository;
import com.connexta.search.query.exceptions.QueryException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;

@Slf4j
public class QueryManagerImpl implements QueryManager {

  @NotNull private final IndexCrudRepository indexCrudRepository;
  @NotBlank private final String endpointUrlRetrieve;

  public QueryManagerImpl(
      @NotNull final IndexCrudRepository indexCrudRepository,
      @NotBlank final String retrieveEndpoint) {
    this.indexCrudRepository = indexCrudRepository;
    this.endpointUrlRetrieve = retrieveEndpoint;
  }

  @Override
  public List<URI> find(String keyword) throws QueryException {
    final List<Index> matchingIndices;
    try {
      matchingIndices = indexCrudRepository.findByContents(keyword);
    } catch (RuntimeException e) {
      // TODO remove this check once solr is deployed independently
      if (e instanceof DataAccessResourceFailureException && indexCrudRepository.count() == 0) {
        log.warn("Solr is empty. Returning empty search results.");
        return Collections.emptyList();
      }

      throw new QueryException("Unable to search for " + keyword, e);
    }

    final List<URI> uris = new ArrayList<>();
    for (final Index index : matchingIndices) {
      final String id = index.getId();
      final URI uri;
      try {
        uri = new URI(endpointUrlRetrieve + id);
      } catch (URISyntaxException e) {
        throw new QueryException(
            "Unable to construct retrieve URI from endpointUrlRetrieve="
                + endpointUrlRetrieve
                + " and id="
                + id,
            e);
      }
      uris.add(uri);
    }

    return Collections.unmodifiableList(uris);
  }
}
