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

import com.connexta.search.index.controllers.IndexController;
import com.connexta.search.index.exceptions.IndexException;
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
import org.geotools.data.DataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@MockBean(DataStore.class)
public class IndexTests {

  @MockBean private SolrClient mockSolrClient;

  @Inject private MockMvc mockMvc;

  @Value("${endpoints.index.version}")
  private String indexApiVersion;

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(ignoreStubs(mockSolrClient));
  }

  @Test
  public void testContextLoads() {}

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

  @Test
  void testInvalidProductId() throws Exception {
    mockMvc
        .perform(
            multipart("/index/1234")
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
  public void testExistingProduct(@Mock final CrudRepository mockCrudRepository) {
    final String id = "00067360b70e4acfab561fe593afaded";
    doReturn(true).when(mockCrudRepository).existsById(id);
    final IndexManager indexManager = new IndexManagerImpl(mockCrudRepository);
    assertThrows(
        IndexException.class,
        () -> indexManager.index(id, "application/json", mock(InputStream.class)));
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
