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
import com.google.common.annotations.VisibleForTesting;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.geotools.filter.Filters;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;

/**
 * Class to encapsulate validating a CommonQL query string and building an OCG Filter object from
 * the string. Create a new instance of this class by passing the CommonQL string into this the
 * constructor. Please note this class is not thread safe. Each thread should create its own
 * instance. Also, this class is not meant to be reused with different query strings and that is why
 * there is not setter for the query string field.
 */
@RequiredArgsConstructor
public class CommonQl {

  @NotNull private final String queryString;
  private Filter filter;

  /**
   * Attempt to validate the query string. Throw an exception is not valid. Check for supported
   * attributes and throw an exception if an unsupported attribute is detected.
   *
   * @return this for fluent interface
   * @throws QueryException
   */
  public CommonQl validate() {
    Set<String> unsupportedAttributes = extractUnsupportedAttributes();
    if (!unsupportedAttributes.isEmpty()) {
      throw new IllegalQueryException(
          String.format(
              "Unsupported query attributes: {%s}", String.join(", ", unsupportedAttributes)));
    }
    return this;
  }

  /**
   * Create the OGC Filter object represented by the query string. Retain the constructed filter in
   * a field. Throw an exception if the query string cannot be parsed.
   *
   * @return OCG Filter
   * @throws MalformedQueryException
   */
  public Filter getFilter() {
    if (null == filter) {
      try {
        filter = CQL.toFilter(queryString);
      } catch (final CQLException e) {
        throw new MalformedQueryException("Invalid CommonQL query string", e);
      }
    }
    return filter;
  }

  /**
   * Collect {@link org.geotools.xml.schema.Attribute} names of query attributes used in the query
   * string that are not supported. If the collection is empty, the query string contains only valid
   * attribute names.
   *
   * @returns List of unsupported attributes names found in the query string
   * @throws MalformedQueryException
   */
  @VisibleForTesting
  Set<String> extractUnsupportedAttributes() {
    Set<String> properties =
        Filters.propertyNames(getFilter()).stream()
            .map(PropertyName::getPropertyName)
            .collect(Collectors.toSet());
    properties.removeAll(SolrConfiguration.QUERY_TERMS);
    return properties;
  }
}
