/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
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
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class IndexTests {

  @MockBean private SolrClient mockSolrClient;

  @Inject private MockMvc mockMvc;

  @After
  public void after() {
    verifyNoMoreInteractions(ignoreStubs(mockSolrClient));
  }

  @Test
  public void testContextLoads() {}

  @Test
  public void testMissingFile() throws Exception {
    mockMvc
        .perform(
            multipart("/mis/product/00067360b70e4acfab561fe593ad3f7a/cst")
                .header("Accept-Version", "0.1.0-SNAPSHOT")
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
  public void testInvalidProductId() throws Exception {
    mockMvc
        .perform(
            multipart("/mis/product/1234/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "0.1.0-SNAPSHOT")
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
  public void testMissingVersion() throws Exception {
    mockMvc
        .perform(
            multipart("/mis/product/00067360b70e4acfab561fe593ad3f7a/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
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
  @Ignore("TODO")
  public void testCantReadAttachment() {
    // TODO verify 400
  }

  @Test
  public void testNonCST() throws Exception {
    mockMvc
        .perform(
            multipart("/mis/product/00067360b70e4acfab561fe593ad3f7a/anotherMetadataType")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
                            StandardCharsets.UTF_8)))
                .header("Accept-Version", "0.1.0-SNAPSHOT")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PUT.toString());
                      return request;
                    })
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isNotFound());
  }

  /** @see SolrClient#query(String, SolrParams, METHOD) */
  @Test
  public void testSolrClientLowLevelIOErrorWhenQuerying() throws Exception {
    final String id = "00067360b70e4acfab561fe593ad3f7a";
    when(mockSolrClient.query(
            eq("searchTerms"),
            argThat(solrQuery -> StringUtils.equals(solrQuery.get("q"), "id:" + id)),
            eq(METHOD.GET)))
        .thenThrow(IOException.class);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
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

  /** @see SolrClient#query(String, SolrParams, METHOD) */
  @Test
  public void testSolrClientErrorOnTheServerWhenQuerying() throws Exception {
    final String id = "00067360b70e4acfab561fe593ad3f7a";
    when(mockSolrClient.query(
            eq("searchTerms"),
            argThat(solrQuery -> StringUtils.equals(solrQuery.get("q"), "id:" + id)),
            eq(METHOD.GET)))
        .thenThrow(SolrServerException.class);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
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

  /** @see SolrClient#query(String, SolrParams, METHOD) */
  @Test
  public void testSolrClientThrowsRuntimeExceptionWhenQuerying() throws Exception {
    final String id = "00067360b70e4acfab561fe593ad3f7a";
    when(mockSolrClient.query(
            eq("searchTerms"),
            argThat(solrQuery -> StringUtils.equals(solrQuery.get("q"), "id:" + id)),
            eq(METHOD.GET)))
        .thenThrow(RuntimeException.class);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
                        IOUtils.toInputStream(
                            "All the color had been leached from Winterfell until only grey and white remained",
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

  /** @see SolrClient#add(String, SolrInputDocument, int) */
  @Test
  public void testSolrClientCommunicationErrorWithTheServerWhenSaving() throws Exception {
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
        "All the color had been leached from Winterfell until only grey and white remained";
    when(mockSolrClient.add(eq("searchTerms"), hasIndexFieldValues(id, contents), anyInt()))
        .thenThrow(IOException.class);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
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

  /** @see SolrClient#add(String, SolrInputDocument, int) */
  @Test
  public void testSolrClientErrorOnTheServerWhenSaving() throws Exception {
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
        "All the color had been leached from Winterfell until only grey and white remained";
    when(mockSolrClient.add(eq("searchTerms"), hasIndexFieldValues(id, contents), anyInt()))
        .thenThrow(SolrServerException.class);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
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

  @Test
  public void testSolrClientThrowsRuntimeExceptionWhenSaving() throws Exception {
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
        "All the color had been leached from Winterfell until only grey and white remained";
    when(mockSolrClient.add(eq("searchTerms"), hasIndexFieldValues(id, contents), anyInt()))
        .thenThrow(RuntimeException.class);

    mockMvc
        .perform(
            multipart("/mis/product/" + id + "/cst")
                .file(
                    new MockMultipartFile(
                        "file",
                        "test_file_name.txt",
                        "text/plain",
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
}
