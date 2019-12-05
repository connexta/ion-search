/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static com.connexta.search.query.configs.QueryStorageAdaptorConfiguration.QUERY_TERMS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.common.exceptions.SearchException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.geotools.data.solr.FilterToSolr;
import org.geotools.filter.Filters;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;

@Slf4j
@AllArgsConstructor
public class SolrQueryStorageAdaptor implements QueryStorageAdaptor {

  public static final String EXT_EXTRACTED_TEXT = "ext.extracted.text";

  @NotNull private final SolrClient solrClient;

  @Override
  public Set<URI> query(String commonQL) {
    Filter filter;
    try {
      filter = ECQL.toFilter(commonQL);
    } catch (CQLException e) {
      try {
        // The ECQL class does not support an equals on id, ie `id=foo`, but CQL does.
        filter = CQL.toFilter(commonQL);
      } catch (CQLException cqle) {
        log.debug("Invalid CommonQL received: {}", commonQL, cqle);
        throw new SearchException(BAD_REQUEST, "Invalid CommonQL received");
      }
    }

    final Set<String> unsupportedAttributes =
        Filters.propertyNames(filter).stream()
            .map(PropertyName::getPropertyName)
            .filter(attribute -> !QUERY_TERMS.contains(attribute))
            .collect(Collectors.toSet());
    if (!unsupportedAttributes.isEmpty()) {
      throw new SearchException(BAD_REQUEST, "Received invalid attributes to query on");
    }

    String solrQueryStr;
    try {
      solrQueryStr = new FilterToSolr(null).encodeToString(filter);
    } catch (Exception e) {
      // TODO add test for this case
      log.debug("Unable to transform CommonQL {} to valid Solr query", commonQL, e);
      throw new SearchException(INTERNAL_SERVER_ERROR, "Error processing CommonQL");
    }

    SolrQuery solrQuery = new SolrQuery(solrQueryStr);
    QueryResponse response;
    try {
      response = solrClient.query(SolrConfiguration.SOLR_COLLECTION, solrQuery);
    } catch (SolrServerException | IOException e) {
      log.debug("Failed to query solr", e);
      throw new SearchException(INTERNAL_SERVER_ERROR, "Error querying");
    }

    final Set<URI> irmURIs = new HashSet<>();
    for (SolrDocument doc : response.getResults()) {
      Object obj = doc.get(SolrConfiguration.IRM_URL_ATTRIBUTE);
      if (obj instanceof String) {
        final String irmUri = (String) obj;
        try {
          irmURIs.add(new URI(irmUri));
        } catch (URISyntaxException e) {
          // TODO write tests for this case
          throw new SearchException(
              INTERNAL_SERVER_ERROR, "Unable to construct URI from irmUri=" + irmUri, e);
        }
      } else {
        // shouldn't hit this since the schema enforces the ID is a string
        log.debug("Skipping invalid solr result {}", doc);
      }
    }

    return irmURIs;
  }
}
