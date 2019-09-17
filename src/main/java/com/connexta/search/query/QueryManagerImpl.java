/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.query.exceptions.QueryException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.FeatureStreams;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.springframework.http.HttpStatus;

@Slf4j
public class QueryManagerImpl implements QueryManager {

  @NotBlank private final String endpointUrlRetrieve;
  @NotNull private final DataStore dataStore;

  public QueryManagerImpl(
      @NotNull final DataStore dataStore, @NotBlank final String retrieveEndpoint) {
    this.endpointUrlRetrieve = retrieveEndpoint;
    this.dataStore = dataStore;
  }

  @Override
  public List<URI> find(final String cqlString) {
    final List<String> matchingIds;
    try {
      matchingIds = doQuery(cqlString);
    } catch (QueryException e) {
      // rethrow for the exception handler to take care of
      throw e;
    } catch (RuntimeException | IOException e) {
      throw new QueryException("Unable to search for " + cqlString, e);
    }

    return Collections.unmodifiableList(getProductUris(matchingIds));
  }

  private List<String> doQuery(@NotBlank final String cqlString) throws IOException {
    final Filter filter = new CommonQl(cqlString).validate().getFilter();
    final SimpleFeatureCollection simpleFeatureCollection =
        dataStore.getFeatureSource(SolrConfiguration.LAYER_NAME).getFeatures(filter);
    final List<Feature> features =
        FeatureStreams.toFeatureStream(simpleFeatureCollection).collect(Collectors.toList());
    return features.stream()
        .map(
            feature ->
                feature.getProperty(SolrConfiguration.ID_ATTRIBUTE_NAME).getValue().toString())
        .collect(Collectors.toList());
  }

  /**
   * This method takes in a list of IDs and creates a list of URIs using the provided {@code
   * endpointUrlRetrieve}
   *
   * @param matchingIds
   * @return A list product retrieve URIs
   */
  private List<URI> getProductUris(List<String> matchingIds) {
    final List<URI> uris = new ArrayList<>();
    for (final String id : matchingIds) {
      final URI uri;
      try {
        uri = new URI(endpointUrlRetrieve + id);
      } catch (URISyntaxException e) {
        throw new QueryException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            String.format(
                "Unable to construct retrieve URI from endpointUrlRetrieve=%s and id=%s",
                endpointUrlRetrieve, id),
            e);
      }
      uris.add(uri);
    }
    return uris;
  }
}
