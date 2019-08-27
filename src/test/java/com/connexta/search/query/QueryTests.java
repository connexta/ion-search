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
import java.net.URISyntaxException;
import java.util.stream.Stream;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

  @ParameterizedTest(name = "{0} request returns {2}")
  @MethodSource("badRequests")
  public void testBadRequests(
      final String requestDescription,
      MockHttpServletRequestBuilder requestBuilder,
      HttpStatus expectedStatus)
      throws Exception {
    mockMvc.perform(requestBuilder).andExpect(status().is(expectedStatus.value()));
  }

  /** @see SolrClient#query(SolrParams, METHOD) */
  @ParameterizedTest
  @ValueSource(classes = {IOException.class, SolrServerException.class, RuntimeException.class})
  public void testSolrClientErrors(Class<? extends Throwable> throwableType) throws Exception {
    when(mockSolrClient.query(eq("searchTerms"), any(SolrQuery.class), eq(SolrRequest.METHOD.GET)))
        .thenThrow(throwableType);

    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath("/search");
    uriBuilder.setParameter("q", "queryKeyword");
    mockMvc
        .perform(MockMvcRequestBuilders.get(uriBuilder.build()))
        .andExpect(status().isInternalServerError());
  }

  private static Stream<Arguments> badRequests() throws URISyntaxException {
    return Stream.of(
        Arguments.of(
            "missing search parameter",
            MockMvcRequestBuilders.get("/search"),
            HttpStatus.BAD_REQUEST),
        Arguments.of(
            "blank keyword",
            MockMvcRequestBuilders.get(
                new URIBuilder().setPath("/search").setParameter("q", "").build()),
            HttpStatus.BAD_REQUEST));
  }
}
