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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.search.common.configs.SolrConfiguration;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opengis.filter.Filter;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@MockBean(SolrClient.class)
public class QueryTests {

  private static final String URI_QUERY_PARAMETER = "q";
  private static final String CONTENTS_LIKE_QUERY_KEYWORD = "contents LIKE 'queryKeyword'";
  private static final String INVALID_CQL_QUERY = "contents SORTALIKE 'metadata'";
  private static final String UNSUPPORTED_TERM_QUERY = "city LIKE 'Paradise City'";
  private static final String SEARCH_ENDPOINT = "/search";
  private static UriComponentsBuilder uriComponentsBuilder;

  @MockBean private DataStore mockDataStore;

  @Inject private MockMvc mockMvc;

  @BeforeAll
  public static void beforeAll() throws URISyntaxException {
    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SEARCH_ENDPOINT);
    uriComponentsBuilder =
        UriComponentsBuilder.fromUri(uriBuilder.build()).query(URI_QUERY_PARAMETER + "={query}");
  }

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(ignoreStubs(mockDataStore));
  }

  @Test
  public void testContextLoads() {}

  @ParameterizedTest(name = "400 Bad Request is returned when {0}")
  @MethodSource("badRequests")
  public void testBadRequests(final String requestDescription, final URI uri) throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get(uri)).andExpect(status().isBadRequest());
  }

  /** @see DataStore#getFeatureSource(String) */
  @ParameterizedTest(
      name = "500 Internal Server Error is returned when DataStore#getFeatureSource throws {0}")
  @ValueSource(classes = {IOException.class, RuntimeException.class})
  public void testGetFeatureSourceErrors(final Class<? extends Throwable> throwableType)
      throws Exception {
    when(mockDataStore.getFeatureSource(eq(SolrConfiguration.LAYER_NAME))).thenThrow(throwableType);
    when(mockDataStore.getTypeNames()).thenReturn(new String[] {"id", "contents"});
    URI uri = uriComponentsBuilder.buildAndExpand(CONTENTS_LIKE_QUERY_KEYWORD).toUri();
    mockMvc.perform(MockMvcRequestBuilders.get(uri)).andExpect(status().isInternalServerError());
  }

  /** @see SimpleFeatureSource#getFeatures(Filter) */
  @ParameterizedTest(
      name =
          "500 Internal Server Error is returned when SimpleFeatureSource#getFeatures throws {0}")
  @ValueSource(classes = {IOException.class, RuntimeException.class})
  public void testGetFeaturesErrors(final Class<? extends Throwable> throwableType)
      throws Exception {
    final SimpleFeatureSource mockSimpleFeatureSource = mock(SimpleFeatureSource.class);
    when(mockSimpleFeatureSource.getFeatures(any(Filter.class))).thenThrow(throwableType);
    when(mockDataStore.getFeatureSource(eq(SolrConfiguration.LAYER_NAME)))
        .thenReturn(mockSimpleFeatureSource);
    URI uri = uriComponentsBuilder.buildAndExpand(CONTENTS_LIKE_QUERY_KEYWORD).toUri();
    mockMvc.perform(MockMvcRequestBuilders.get(uri)).andExpect(status().isInternalServerError());
  }

  private static Stream<Arguments> badRequests() throws URISyntaxException {
    return Stream.of(
        Arguments.of("missing query parameter", new URI("/search")),
        Arguments.of("blank query string", uriComponentsBuilder.buildAndExpand("").toUri()),
        Arguments.of(
            "unsupported query attribute",
            uriComponentsBuilder.buildAndExpand(UNSUPPORTED_TERM_QUERY).toUri()),
        Arguments.of(
            "invalid CommonQL query",
            uriComponentsBuilder.buildAndExpand(INVALID_CQL_QUERY).toUri()));
  }
}
