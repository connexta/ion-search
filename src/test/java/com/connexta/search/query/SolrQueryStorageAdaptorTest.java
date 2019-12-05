/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static com.connexta.search.common.configs.SolrConfiguration.IRM_URL_ATTRIBUTE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.query.configs.QueryStorageAdaptorConfiguration;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SolrQueryStorageAdaptorTest {

  @Mock private SolrClient mockSolrClient;

  private QueryStorageAdaptor queryStorageAdaptor;

  @BeforeEach
  void beforeEach() {
    queryStorageAdaptor = new SolrQueryStorageAdaptor(mockSolrClient);
  }

  // query tests

  @Test
  void testQueryInvalidEcql() {
    // expect
    SearchException e =
        assertThrows(SearchException.class, () -> queryStorageAdaptor.query("thisIsInvalidEcql"));

    assertThat(e.getMessage(), containsString("Invalid CommonQL received"));
    assertThat(e.getStatus(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  void testSupportedAttributes() throws Exception {
    // setup
    final String template = "%s = '%s'";
    final Map<String, String> queryPairs = new HashMap<>();
    queryPairs.put("contents", "lots of words");
    queryPairs.put("country_code", "USA");
    queryPairs.put("created", "2019-11-13");
    queryPairs.put("expiration", "2119-11-01");
    queryPairs.put("icid", "floop");
    queryPairs.put("id", "bloop");
    queryPairs.put("keyword", "key");
    queryPairs.put("modified", "2019-11-14");
    queryPairs.put("title", "A Title");

    assertThat(
        "Expected attributes do not match actual attributes. These collections need to be the same.",
        List.of(queryPairs.keySet()),
        containsInAnyOrder(QueryStorageAdaptorConfiguration.QUERY_TERMS));

    String queryString =
        queryPairs.entrySet().stream()
            .map(e -> String.format(template, e.getKey(), e.getValue()))
            .collect(Collectors.joining(" AND "));
    QueryResponse queryResponse = mockQueryResponse("something");
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    Set<URI> result = queryStorageAdaptor.query(queryString);
    assertThat(result, containsInAnyOrder(new URI("something")));
  }

  @Test
  void testUnsupportedAttributesPresent() {
    // setup
    final String unsupportedAttrQuery = "thisAttributeIsNotSupported = 'value'";

    // expect
    SearchException e =
        assertThrows(SearchException.class, () -> queryStorageAdaptor.query(unsupportedAttrQuery));

    assertThat(e.getMessage(), containsString("Received invalid attributes to query on"));
    assertThat(e.getStatus(), is(HttpStatus.BAD_REQUEST));
  }

  @ParameterizedTest
  @ValueSource(classes = {SolrServerException.class, IOException.class})
  void testQueryException(final Class<? extends Throwable> throwableType) throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    when(mockSolrClient.query(anyString(), any())).thenThrow(throwableType);

    // expect
    SearchException e =
        assertThrows(SearchException.class, () -> queryStorageAdaptor.query(idQuery));

    assertThat(e.getMessage(), containsString("Error querying"));
    assertThat(e.getStatus(), is(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void testInvalidIrmUriString() throws Exception {
    // setup
    final String idQuery = "id = 'value1' AND id = 'value2'";

    QueryResponse queryResponse =
        mockQueryResponse(new URI("valid").toString(), "<this uri is invalid>");
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    final SearchException thrown =
        assertThrows(SearchException.class, () -> queryStorageAdaptor.query(idQuery));
    assertThat(thrown.getStatus(), is(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void testQueryNoResults() throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    QueryResponse queryResponse = mockQueryResponse();
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    assertThat(queryStorageAdaptor.query(idQuery), is(empty()));
  }

  @Test
  void testQueryManyResults() throws Exception {
    // setup
    final String idQuery = "id = 'value1' AND id = 'value2'";

    final URI irmUri1 = new URI("value1");
    final URI irmUri2 = new URI("value2");
    QueryResponse queryResponse = mockQueryResponse(irmUri1.toString(), irmUri2.toString());
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    assertThat(queryStorageAdaptor.query(idQuery), containsInAnyOrder(irmUri1, irmUri2));
  }

  private static QueryResponse mockQueryResponse(final String... irmUriStrings) {
    List<SolrDocument> solrDocuments = new ArrayList<>();
    for (final String irmUri : irmUriStrings) {
      SolrDocument document = mock(SolrDocument.class);
      when(document.get(IRM_URL_ATTRIBUTE)).thenReturn(irmUri);
      solrDocuments.add(document);
    }

    SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.addAll(solrDocuments);

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(solrDocumentList);
    return queryResponse;
  }
}
