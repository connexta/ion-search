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

import com.connexta.search.common.IndexRepository;
import com.connexta.search.common.SearchManager;
import com.connexta.search.common.SearchManagerImpl;
import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.index.controllers.IndexController;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** TODO Update this to component and unit tests */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class IndexITest {

  @MockBean private SolrClient mockSolrClient;

  @Inject private MockMvc mockMvc;

  @Value("${endpoints.index.version}")
  private String indexApiVersion;

  @AfterEach
  void after() {
    verifyNoMoreInteractions(ignoreStubs(mockSolrClient));
  }

  @Test
  void testMissingFile() throws Exception {
    mockMvc
        .perform(
            multipart("/index/00067360b70e4acfab561fe593ad3f7a")
                .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest(name = "400 Bad Request if ID is {0}")
  @NullSource
  @ValueSource(
      strings = {"   ", "1234567890123456789012345678901234", "+0067360b70e4acfab561fe593ad3f7a"})
  void testInvalidDatasetId(final String id) throws Exception {
    mockMvc
        .perform(
            multipart("/index/" + id)
                .file(
                    new MockMultipartFile(
                        "file",
                        "this originalFilename is ignored",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testMissingAcceptVersion() throws Exception {
    mockMvc
        .perform(
            multipart("/index/00067360b70e4acfab561fe593ad3f7a")
                .file(
                    new MockMultipartFile(
                        "file",
                        "this originalFilename is ignored",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testBadPath() throws Exception {
    mockMvc
        .perform(
            multipart("/index/00067360b70e4acfab561fe593ad3f7a/badpath")
                .file(
                    new MockMultipartFile(
                        "file",
                        "this originalFilename is ignored",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isNotFound());
  }

  // TODO: Pull out accept version validation in its own class.
  @Test
  void testInvalidAcceptVersion() throws Exception {
    mockMvc
        .perform(
            multipart("/index/00067360b70e4acfab561fe593ad3f7a")
                .file(
                    new MockMultipartFile(
                        "file",
                        "this originalFilename is ignored",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .header(IndexController.ACCEPT_VERSION_HEADER_NAME, "this version is invalid")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isNotImplemented());
  }

  @Test
  @Disabled("TODO")
  void testCantReadAttachment() {
    // TODO verify 400
  }

  /** @see SolrClient#query(String, SolrParams, METHOD) */
  @ParameterizedTest
  @ValueSource(classes = {IOException.class, SolrServerException.class, RuntimeException.class})
  void testSolrClientErrorsWhenQuerying(Class<? extends Throwable> throwableType) throws Exception {
    final String id = "00067360b70e4acfab561fe593ad3f7a";
    when(mockSolrClient.query(
            eq("search_terms"),
            argThat(solrQuery -> StringUtils.equals(solrQuery.get("q"), "id:" + id)),
            eq(METHOD.GET)))
        .thenThrow(throwableType);

    mockMvc
        .perform(
            multipart("/index/" + id)
                .file(
                    new MockMultipartFile(
                        "file",
                        "this originalFilename is ignored",
                        "application/json",
                        IOUtils.toInputStream(
                            "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}",
                            StandardCharsets.UTF_8)))
                .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
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
  void testSolrClientErrorsWhenSaving(final Class<? extends Throwable> throwableType)
      throws Exception {
    final String id = "00067360b70e4acfab561fe593ad3f7a";

    final SolrDocumentList mockSolrDocumentList = mock(SolrDocumentList.class);
    when(mockSolrDocumentList.size()).thenReturn(0);
    final QueryResponse mockQueryResponse = mock(QueryResponse.class);
    when(mockQueryResponse.getResults()).thenReturn(mockSolrDocumentList);
    when(mockSolrClient.query(
            eq("search_terms"),
            argThat(solrQuery -> StringUtils.equals(solrQuery.get("q"), "id:" + id)),
            eq(METHOD.GET)))
        .thenReturn(mockQueryResponse);

    final String contents =
        "{\"ext.extracted.text\" : \"All the color had been leached from Winterfell until only grey and white remained\"}";
    when(mockSolrClient.add(eq("search_terms"), hasIndexFieldValues(id, contents), anyInt()))
        .thenThrow(throwableType);

    mockMvc
        .perform(
            multipart("/index/" + id)
                .file(
                    new MockMultipartFile(
                        "file",
                        "this originalFilename is ignored",
                        "application/json",
                        IOUtils.toInputStream(contents, StandardCharsets.UTF_8)))
                .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void testExistingDatasetId(@Mock final IndexRepository indexRepository) {
    final String id = "00067360b70e4acfab561fe593afaded";
    doReturn(true).when(indexRepository).existsById(id);
    final SearchManager searchManager = new SearchManagerImpl(indexRepository, null);
    assertThrows(
        SearchException.class,
        () -> searchManager.index(id, "application/json", mock(InputStream.class)));
  }

  private static SolrInputDocument hasIndexFieldValues(
      @NotEmpty final String id, @NotNull final String contents) {
    return argThat(
        solrInputDocument ->
            StringUtils.equals((String) solrInputDocument.getField("id").getValue(), id)
                && StringUtils.equals(
                    (String) solrInputDocument.getField("contents").getValue(), contents));
  }
}
