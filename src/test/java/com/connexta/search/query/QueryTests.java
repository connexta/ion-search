/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import javax.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class QueryTests {

  @MockBean private SolrClient mockSolrClient;

  @Inject private MockMvc mockMvc;

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(ignoreStubs(mockSolrClient));
  }

  @Test
  public void testContextLoads() {}

  @Test
  public void testBadRequest() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/search")).andExpect(status().isBadRequest());
  }

  @Test
  public void testEmptyKeyword() throws Exception {
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", "");
    mockMvc
        .perform(MockMvcRequestBuilders.get(queryUriBuilder.build()))
        .andExpect(status().isBadRequest());
  }

  /** @see SolrClient#query(SolrParams, METHOD) */
  @ParameterizedTest
  @ValueSource(classes = {IOException.class, SolrServerException.class, RuntimeException.class})
  public void testSolrClientErrors(Class<? extends Throwable> throwable) throws Exception {
    when(mockSolrClient.query(eq("searchTerms"), any(SolrQuery.class), eq(SolrRequest.METHOD.GET)))
        .thenThrow(throwable);

    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath("/search");
    uriBuilder.setParameter("q", "queryKeyword");
    mockMvc
        .perform(MockMvcRequestBuilders.get(uriBuilder.build()))
        .andExpect(status().isInternalServerError());
  }
}
