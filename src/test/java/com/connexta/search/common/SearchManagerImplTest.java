/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static com.connexta.search.common.SearchManagerImpl.EXT_EXTRACTED_TEXT;
import static com.connexta.search.common.configs.SolrConfiguration.IRM_URI_STRING_ATTRIBUTE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.connexta.search.common.exceptions.SearchException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
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

  // index tests

  @Test
  public void testExistingDataset(@Mock final InputStream mockInputStream) {
    // given
    final SearchManager indexManager = new SearchManagerImpl(mockIndexRepository, mockSolrClient);
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";

    // and stub dataset already exists
    when(mockIndexRepository.existsById(datasetId)).thenReturn(true);

    // expect
    assertThrows(
        SearchException.class,
        () ->
            indexManager.index(
                datasetId,
                new URI(String.format("http://store:9041/dataset/%s/irm", datasetId)),
                mockInputStream));

    // and
    verifyNoMoreInteractions(mockIndexRepository, mockSolrClient, mockInputStream);
  }

  @Test
  public void testExceptionWhenCheckingIfDatasetExists(@Mock final InputStream mockInputStream) {
    // given
    final SearchManager indexManager = new SearchManagerImpl(mockIndexRepository, mockSolrClient);
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";

    // and stub dataset already exists
    final RuntimeException runtimeException = new RuntimeException();
    doThrow(runtimeException).when(mockIndexRepository).existsById(datasetId);

    // expect
    final SearchException thrown =
        assertThrows(
            SearchException.class,
            () ->
                indexManager.index(
                    datasetId,
                    new URI(String.format("http://store:9041/dataset/%s/irm", datasetId)),
                    mockInputStream));
    assertThat(thrown.getCause(), is(runtimeException));

    // and
    verifyNoMoreInteractions(mockIndexRepository, mockSolrClient, mockInputStream);
  }

  /** TODO change this from IRM to CST */
  @ParameterizedTest
  @ValueSource(strings = {"", "{}", "{ \"\": \"text\"}", "this isn't json"})
  public void testIrmFormatIsInvalid(final String body) {
    // given
    final SearchManager indexManager = new SearchManagerImpl(mockIndexRepository, mockSolrClient);
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";

    // and stub CrudRepository
    when(mockIndexRepository.existsById(datasetId)).thenReturn(false);

    // expect
    assertThrows(
        SearchException.class,
        () ->
            indexManager.index(
                datasetId,
                new URI(String.format("http://store:9041/dataset/%s/irm", datasetId)),
                IOUtils.toInputStream(body, StandardCharsets.UTF_8)));
  }

  @Test
  public void testExceptionWhenSaving() throws Exception {
    // given
    final SearchManager searchManager = new SearchManagerImpl(mockIndexRepository, mockSolrClient);
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final String contents =
        "All the color had been leached from Winterfell until only grey and white remained.";

    final URI irmUri = new URI(String.format("http://store:9041/dataset/%s/irm", datasetId));

    // and stub CrudRepository#existsById
    when(mockIndexRepository.existsById(datasetId)).thenReturn(false);

    // and stub CrudRepository#save
    final RuntimeException runtimeException = new RuntimeException();
    doThrow(runtimeException)
        .when(mockIndexRepository)
        .save(argThat(index -> index.equals(new Index(datasetId, contents, irmUri.toString()))));

    // expect
    final SearchException thrown =
        assertThrows(
            SearchException.class,
            () ->
                searchManager.index(
                    datasetId,
                    irmUri,
                    IOUtils.toInputStream(
                        String.format("{\"%s\" : \"%s\"}", EXT_EXTRACTED_TEXT, contents),
                        StandardCharsets.UTF_8)));
    assertThat(thrown.getCause(), is(runtimeException));
  }

  @Test
  public void testIndex() throws Exception {
    // given
    final SearchManager searchManager = new SearchManagerImpl(mockIndexRepository, mockSolrClient);
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final String contents =
        "All the color had been leached from Winterfell until only grey and white remained.";

    final URI irmUri = new URI(String.format("http://store:9041/dataset/%s/irm", datasetId));

    // and stub CrudRepository
    when(mockIndexRepository.existsById(datasetId)).thenReturn(false);

    // when
    searchManager.index(
        datasetId,
        irmUri,
        IOUtils.toInputStream(
            String.format("{\"%s\" : \"%s\"}", EXT_EXTRACTED_TEXT, contents),
            StandardCharsets.UTF_8));

    // then
    verify(mockIndexRepository)
        .save(argThat(index -> index.equals(new Index(datasetId, contents, irmUri.toString()))));
  }

  // query tests

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

    final URI irmUri = new URI("value");
    QueryResponse queryResponse = mockQueryResponse(irmUri);
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    assertThat(searchManagerImpl.query(idQuery), contains(irmUri));
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

    final URI irmUri1 = new URI("value1");
    final URI irmUri2 = new URI("value2");
    QueryResponse queryResponse = mockQueryResponse(irmUri1, irmUri2);
    when(mockSolrClient.query(anyString(), any())).thenReturn(queryResponse);

    // expect
    assertThat(searchManagerImpl.query(idQuery), containsInAnyOrder(irmUri1, irmUri2));
  }

  private static QueryResponse mockQueryResponse(final URI... irmUris) {
    List<SolrDocument> solrDocuments = new ArrayList<>();
    for (final URI irmUri : irmUris) {
      SolrDocument document = mock(SolrDocument.class);
      when(document.get(IRM_URI_STRING_ATTRIBUTE)).thenReturn(irmUri.toString());
      solrDocuments.add(document);
    }

    SolrDocumentList solrDocumentList = new SolrDocumentList();
    solrDocumentList.addAll(solrDocuments);

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(solrDocumentList);
    return queryResponse;
  }
}
