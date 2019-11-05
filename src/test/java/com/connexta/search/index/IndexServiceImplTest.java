/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.index.exceptions.IndexException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.geotools.data.solr.FilterToSolr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndexServiceImplTest {

  @Mock private IndexRepository indexRepository;

  @Mock private SolrClient solrClient;

  private IndexServiceImpl indexServiceImpl;

  @BeforeEach
  void beforeEach() {
    indexServiceImpl = new IndexServiceImpl(indexRepository, solrClient);
  }

  @Test
  void testQueryInvalidEcql() {
    // expect
    IndexException e =
        assertThrows(IndexException.class, () -> indexServiceImpl.query("thisIsInvalidEcql"));
    assertTrue(e.getMessage().contains("Invalid CQL received"));
    assertEquals(400, e.getStatus().value());
  }

  @Test
  void testQueryIdEquals() throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    QueryResponse queryResponse = mockQueryResponse("value");
    when(solrClient.query(anyString(), any())).thenReturn(queryResponse);

    // when
    Set<String> ids = indexServiceImpl.query(idQuery);

    // then
    assertEquals(Set.of("value"), ids);
  }

  @Test
  void testUnsupportedAttributesPresent() {
    // setup
    final String unsupportedAttrQuery = "thisAttributeIsNotSupported = 'value'";

    // expect
    IndexException e =
        assertThrows(IndexException.class, () -> indexServiceImpl.query(unsupportedAttrQuery));

    assertTrue(e.getMessage().contains("Received invalid attributes to index on"));
    assertEquals(400, e.getStatus().value());
  }

  @Test
  void testFilterToSolrQueryFails() throws Exception {
    // setup
    FilterToSolr filterToSolr = mock(FilterToSolr.class);
    when(filterToSolr.encodeToString(any())).thenThrow(Exception.class);

    final String cql = "id = 'value'";

    // expect
    IndexException e =
        assertThrows(IndexException.class, () -> indexServiceImpl.query(cql, filterToSolr));

    assertTrue(e.getMessage().contains("Error processing CQL"));
    assertEquals(500, e.getStatus().value());
  }

  @Test
  void testQuerySolrServerException() throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    when(solrClient.query(anyString(), any())).thenThrow(SolrServerException.class);

    // expect
    IndexException e = assertThrows(IndexException.class, () -> indexServiceImpl.query(idQuery));

    assertTrue(e.getMessage().contains("Error querying index"));
    assertEquals(500, e.getStatus().value());
  }

  @Test
  void testQuerySolrIOException() throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    when(solrClient.query(anyString(), any())).thenThrow(IOException.class);

    // expect
    IndexException e = assertThrows(IndexException.class, () -> indexServiceImpl.query(idQuery));

    assertTrue(e.getMessage().contains("Error querying index"));
    assertEquals(500, e.getStatus().value());
  }

  @Test
  void testQueryNoResults() throws Exception {
    // setup
    final String idQuery = "id = 'value'";

    QueryResponse queryResponse = mockQueryResponse();
    when(solrClient.query(anyString(), any())).thenReturn(queryResponse);

    // when
    Set<String> ids = indexServiceImpl.query(idQuery);

    // then
    assertEquals(Set.of(), ids);
  }

  @Test
  void testQueryManyResults() throws Exception {
    // setup
    final String idQuery = "id = 'value1' AND id = 'value2'";

    QueryResponse queryResponse = mockQueryResponse("value1", "value2");
    when(solrClient.query(anyString(), any())).thenReturn(queryResponse);

    // when
    Set<String> ids = indexServiceImpl.query(idQuery);

    // then
    assertEquals(Set.of("value1", "value2"), ids);
  }

  private QueryResponse mockQueryResponse(String... queryIds) {
    List<SolrDocument> solrDocuments = new ArrayList<>();
    for (String queryId : queryIds) {
      SolrDocument document = mock(SolrDocument.class);
      when(document.get(SolrConfiguration.ID_ATTRIBUTE_NAME)).thenReturn(queryId);
      solrDocuments.add(document);
    }

    SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.addAll(solrDocuments);

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(solrDocumentList);
    return queryResponse;
  }
}
