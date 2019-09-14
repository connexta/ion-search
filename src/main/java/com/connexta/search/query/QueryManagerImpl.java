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
import org.apache.commons.lang3.StringUtils;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.util.FeatureStreams;
import org.geotools.filter.Filters;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
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
    final Filter filter;
    try {
      filter = CQL.toFilter(cqlString);
    } catch (final CQLException e) {
      throw new MalformedQueryException("Invalid CQL query string", e);
    }
    List<String> unsupportedAttributes = getUnsupportedQueryAttributes(filter);
    if (!unsupportedAttributes.isEmpty()) {
      throw new IllegalQueryException(
          String.format(
              "Unsupported query attributes: {%s}", String.join(", ", unsupportedAttributes)));
    }
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
   * @return
   */
  public List<URI> getProductUris(List<String> matchingIds) {
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

  /**
   * A helper method that collects all of the {@link org.geotools.xml.schema.Attribute} names from a
   * {@link Filter} and compares them to the list of {@link SolrConfiguration} query terms.
   *
   * @param filter
   * @return
   */
  public List<String> getUnsupportedQueryAttributes(Filter filter) {
    List<String> unsupportedAttributes = new ArrayList<>();
    Set<String> supportedAttributes = SolrConfiguration.QUERY_TERMS;
    for (PropertyName propertyName : Filters.propertyNames(filter)) {
      String propertyNameString = propertyName.getPropertyName();
      if (supportedAttributes.stream()
          .noneMatch(attributeName -> StringUtils.equals(attributeName, propertyNameString))) {
        unsupportedAttributes.add(propertyNameString);
      }
    }
    return unsupportedAttributes;
  }
}
