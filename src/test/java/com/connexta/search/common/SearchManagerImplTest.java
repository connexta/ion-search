/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static com.connexta.search.common.configs.SolrConfiguration.ID_ATTRIBUTE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.connexta.search.common.exceptions.SearchException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.geotools.data.solr.FilterToSolr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SearchManagerImplTest {

  @Mock private IndexRepository mockIndexRepository;
  @Mock private SolrClient mockSolrClient;

  private SearchManagerImpl searchManagerImpl;

  @BeforeEach
  void beforeEach() {
    searchManagerImpl = new SearchManagerImpl(mockIndexRepository, mockSolrClient);
  }

  @Test
  void testQueryInvalidEcql() {
    // expect
    SearchException e =
        assertThrows(SearchException.class, () -> searchManagerImpl.query("thisIsInvalidEcql"));

    assertThat(e.getMessage(), containsString("Invalid CQL received"));
    assertThat(e.getStatus(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  void testQueryIdEquals() throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    QueryResponse queryResponse = mockQueryResponse("value");
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    assertThat(searchManagerImpl.query(idQuery), contains("value"));
  }

  @Test
  void testUnsupportedAttributesPresent() {
    // setup
    final String unsupportedAttrQuery = "thisAttributeIsNotSupported = 'value'";

    // expect
    SearchException e =
        assertThrows(SearchException.class, () -> searchManagerImpl.query(unsupportedAttrQuery));

    assertThat(e.getMessage(), containsString("Received invalid attributes to index on"));
    assertThat(e.getStatus(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  void testFilterToSolrQueryFails() throws Exception {
    // setup
    FilterToSolr filterToSolr = mock(FilterToSolr.class);
    when(filterToSolr.encodeToString(any())).thenThrow(Exception.class);

    final String cql = "id = 'value'";

    // expect
    SearchException e =
        assertThrows(SearchException.class, () -> searchManagerImpl.query(cql, filterToSolr));

    assertThat(e.getMessage(), containsString("Error processing CQL"));
    assertThat(e.getStatus(), is(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @ParameterizedTest
  @ValueSource(classes = {SolrServerException.class, IOException.class})
  void testQueryException(final Class<? extends Throwable> throwableType) throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    when(mockSolrClient.query(anyString(), any())).thenThrow(throwableType);

    // expect
    SearchException e = assertThrows(SearchException.class, () -> searchManagerImpl.query(idQuery));

    assertThat(e.getMessage(), containsString("Error querying index"));
    assertThat(e.getStatus(), is(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void testQueryNoResults() throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    QueryResponse queryResponse = mockQueryResponse();
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    assertThat(searchManagerImpl.query(idQuery), is(empty()));
  }

  @Test
  void testQueryManyResults() throws Exception {
    // setup
    final String idQuery = "id = 'value1' AND id = 'value2'";

    QueryResponse queryResponse = mockQueryResponse("value1", "value2");
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    assertThat(searchManagerImpl.query(idQuery), containsInAnyOrder("value1", "value2"));
  }

  private static QueryResponse mockQueryResponse(String... queryIds) {
    List<SolrDocument> solrDocuments = new ArrayList<>();
    for (String queryId : queryIds) {
      SolrDocument document = mock(SolrDocument.class);
      when(document.get(ID_ATTRIBUTE)).thenReturn(queryId);
      solrDocuments.add(document);
    }

    SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.addAll(solrDocuments);

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(solrDocumentList);
    return queryResponse;
  }
}
