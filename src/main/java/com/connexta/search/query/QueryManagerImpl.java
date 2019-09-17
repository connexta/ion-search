/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.query.exceptions.IllegalQueryException;
import com.connexta.search.query.exceptions.MalformedQueryException;
import com.connexta.search.query.exceptions.QueryException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.FeatureStreams;
import org.geotools.filter.Filters;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;

@Slf4j
public class QueryManagerImpl implements QueryManager {

  @NotBlank private final String endpointUrlRetrieve;
  @NotNull private final DataStore dataStore;

  public QueryManagerImpl(
      @NotNull final DataStore dataStore, @NotBlank final String retrieveEndpoint) {
    this.endpointUrlRetrieve = retrieveEndpoint;
    this.dataStore = dataStore;
  }

  /**
   * Create the OGC Filter object represented by the query string. Throw an exception if the query
   * string cannot be parsed or contains unsupported attributes
   *
   * @return OCG Filter * @throws IllegalQueryException * @throws MalformedQueryException * @throws
   * @throws IllegalQueryException
   * @throws MalformedQueryException
   */
  public static Filter getFilter(final String queryString)
      throws MalformedQueryException, IllegalQueryException {
    final Filter filter;
    try {
      filter = CQL.toFilter(queryString);
    } catch (final CQLException e) {
      throw new MalformedQueryException(e);
    }

    final Set<String> unsupportedAttributes =
        Filters.propertyNames(filter).stream()
            .map(PropertyName::getPropertyName)
            .collect(Collectors.toSet());
    unsupportedAttributes.removeAll(
        com.connexta.search.common.configs.SolrConfiguration.QUERY_TERMS);
    if (!unsupportedAttributes.isEmpty()) {
      throw new IllegalQueryException(unsupportedAttributes);
    }

    return filter;
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
    final Filter filter = getFilter(cqlString);
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
