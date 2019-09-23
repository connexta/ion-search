/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static com.connexta.search.common.configs.SolrConfiguration.QUERY_TERMS;
import static org.apache.commons.lang3.StringUtils.contains;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.query.exceptions.IllegalQueryException;
import com.connexta.search.query.exceptions.MalformedQueryException;
import com.connexta.search.query.exceptions.QueryException;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

  @NotBlank private final String productRetrieveEndpoint;
  @NotNull private final DataStore dataStore;

  public QueryManagerImpl(
      @NotNull final DataStore dataStore, @NotBlank final String productRetrieveEndpoint) {
    this.productRetrieveEndpoint = productRetrieveEndpoint;
    this.dataStore = dataStore;
  }

  /**
   * Creates the {@link Filter} represented by the {@code queryString}
   *
   * @throws MalformedQueryException if the {@code queryString} cannot be parsed
   * @throws IllegalQueryException if the {@code queryString} contains unsupported attributes
   */
  @VisibleForTesting
  static Filter getFilter(final String queryString)
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
            .filter(attribute -> !QUERY_TERMS.contains(attribute))
            .collect(Collectors.toSet());
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
      if (isKnownError(e)) {
        return Collections.emptyList();
      } else {
        throw new QueryException("Unable to search for " + cqlString, e);
      }
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
   * Creates a {@link List} of Product retrieve {@link URI}s from the {@code matchingIds} using the
   * provided {@link #productRetrieveEndpoint}
   *
   * @param matchingIds may be empty
   * @return A {@link List} of product retrieve {@link URI}s
   * @throws QueryException if unable to construct a retrieve URI
   */
  private List<URI> getProductUris(final List<String> matchingIds) {
    return matchingIds.stream()
        .map(
            id -> {
              try {
                return new URI(productRetrieveEndpoint + id);
              } catch (URISyntaxException e) {
                throw new QueryException(
                    String.format(
                        "Unable to construct retrieve URI from endpointUrlProductRetrieve=%s and id=%s",
                        productRetrieveEndpoint, id),
                    e);
              }
            })
        .collect(Collectors.toList());
  }

  private boolean isKnownError(Exception e) {
    // The DataStore throws an exception of the the Solr core has no documents (schema?)
    // In such a case, return zero results.
    Throwable cause = e.getCause();
    while (cause != null) {
      if (contains(
          cause.getMessage(),
          "Cursor functionality requires a sort containing a uniqueKey field tie breaker")) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }
}
