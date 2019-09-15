/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.search.index.exceptions.IndexException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.StringUtils;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class IndexTests {

  @MockBean private SolrClient mockSolrClient;

  @Inject private MockMvc mockMvc;

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(ignoreStubs(mockSolrClient));
  }

  @Test
  public void testContextLoads() {}

  @ParameterizedTest(name = "{0} index request returns {2}")
  @MethodSource("badRequests")
  void testBadRequests(
      final String requestDescription,
      final MockHttpServletRequestBuilder requestBuilder,
      final HttpStatus expectedStatus)
      throws Exception {
    mockMvc
        .perform(
            requestBuilder
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().is(expectedStatus.value()));
  }

  @Test
  @Disabled("TODO")
  public void testCantReadAttachment() {
    // TODO verify 400
  }

  /** @see SolrClient#query(String, SolrParams, METHOD) */
  @ParameterizedTest
  @ValueSource(classes = {IOException.class, SolrServerException.class, RuntimeException.class})
  public void testSolrClientErrorsWhenQuerying(Class<? extends Throwable> throwableType)
      throws Exception {
    final String id = "00067360b70e4acfab561fe593ad3f7a";
    when(mockSolrClient.query(
            eq("searchTerms"),
            argThat(solrQuery -> StringUtils.equals(solrQuery.get("q"), "id:" + id)),
            eq(METHOD.GET)))
        .thenThrow(throwableType);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.json",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "0.1.0-SNAPSHOT")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }

  // TODO fix this test. It fails for the wrong reason
  /** @see SolrClient#add(String, SolrInputDocument, int) */
  @Disabled
  @ParameterizedTest
  @ValueSource(classes = {IOException.class, SolrServerException.class, RuntimeException.class})
  public void testSolrClientErrorsWhenSaving(final Class<? extends Throwable> throwableType)
      throws Exception {
    final String id = "00067360b70e4acfab561fe593ad3f7a";

    final SolrDocumentList mockSolrDocumentList = mock(SolrDocumentList.class);
    when(mockSolrDocumentList.size()).thenReturn(0);
    final QueryResponse mockQueryResponse = mock(QueryResponse.class);
    when(mockQueryResponse.getResults()).thenReturn(mockSolrDocumentList);
    when(mockSolrClient.query(
            eq("searchTerms"),
            argThat(solrQuery -> StringUtils.equals(solrQuery.get("q"), "id:" + id)),
            eq(METHOD.GET)))
        .thenReturn(mockQueryResponse);

    final String contents =
        "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}";
    when(mockSolrClient.add(eq("searchTerms"), hasIndexFieldValues(id, contents), anyInt()))
        .thenThrow(throwableType);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.json",
                        "application/json",
                        IOUtils.toInputStream(contents, StandardCharsets.UTF_8)))
                .header("Accept-Version", "0.1.0-SNAPSHOT")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }

  private static SolrInputDocument hasIndexFieldValues(
      @NotEmpty final String id, @NotNull final String contents) {
    return argThat(
        solrInputDocument ->
            StringUtils.equals((String) solrInputDocument.getField("id").getValue(), id)
                && StringUtils.equals(
                    (String) solrInputDocument.getField("contents").getValue(), contents));
  }

  private static Stream<Arguments> badRequests() throws IOException {
    // TODO test can't read attachment
    return Stream.of(
        Arguments.of(
            "missing file",
            multipart("/mis/product/00067360b70e4acfab561fe593ad3f7a/cst")
                .header("Accept-Version", "0.1.0-SNAPSHOT"),
            HttpStatus.BAD_REQUEST),
        Arguments.of(
            "invalid productId",
            multipart("/mis/product/1234/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.json",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "0.1.0-SNAPSHOT"),
            HttpStatus.BAD_REQUEST),
        Arguments.of(
            "missing Accept-Version",
            multipart("/mis/product/00067360b70e4acfab561fe593ad3f7a/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.json",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8))),
            HttpStatus.BAD_REQUEST),
        Arguments.of(
            "not cst",
            multipart("/mis/product/00067360b70e4acfab561fe593ad3f7a/anotherMetadataType")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.json",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "0.1.0-SNAPSHOT"),
            HttpStatus.NOT_FOUND));
  }

  @Test
  public void testExistingProduct() {
    CrudRepository crudRepository = mock(CrudRepository.class);
    String productId = "00067360b70e4acfab561fe593afaded";
    doReturn(true).when(crudRepository).existsById(productId);
    IndexManagerImpl indexManager = new IndexManagerImpl(crudRepository);
    assertThrows(
        IndexException.class,
        () -> indexManager.index(productId, "application/json", mock(InputStream.class)));
  }
}
