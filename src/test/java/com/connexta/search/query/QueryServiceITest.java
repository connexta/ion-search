/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.search.query.controllers.QueryController;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class uses {@link SpringBootTest} to fully start the container. The primary purpose is to
 * test the {@link QueryService}, but the test requests go through the {@link
 * com.connexta.search.query.controllers.QueryController} to reach the {@link QueryService}.
 *
 * <p>TODO Update this to component and unit tests
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@MockBean(SolrClient.class)
class QueryServiceITest {

  private static final String URI_QUERY_PARAMETER = "q";
  private static final String INVALID_CQL_QUERY = "contents notACqlOperator 'metadata'";
  private static final String UNSUPPORTED_TERM_QUERY = "anSupportedQueryTerm LIKE 'Paradise City'";
  private static UriComponentsBuilder uriComponentsBuilder;

  @Inject private MockMvc mockMvc;

  @BeforeAll
  static void beforeAll() throws URISyntaxException {
    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(QueryController.URL_TEMPLATE);
    uriComponentsBuilder =
        UriComponentsBuilder.fromUri(uriBuilder.build()).query(URI_QUERY_PARAMETER + "={query}");
  }

  @ParameterizedTest(name = "400 Bad Request is returned when {0}")
  @MethodSource("badRequests")
  void testBadRequests(final URI uri) throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get(uri)).andExpect(status().isBadRequest());
  }

  // TODO: Move query validation logic into its own class.
  private static Stream<Arguments> badRequests() throws URISyntaxException {
    String longString = "x".repeat(5001);
    return Stream.of(
        Arguments.of(new URI(QueryController.URL_TEMPLATE)),
        Arguments.of(uriComponentsBuilder.buildAndExpand("").toUri()),
        Arguments.of(uriComponentsBuilder.buildAndExpand(UNSUPPORTED_TERM_QUERY).toUri()),
        Arguments.of(uriComponentsBuilder.buildAndExpand(INVALID_CQL_QUERY).toUri()),
        Arguments.of(uriComponentsBuilder.buildAndExpand(longString).toUri()));
  }
}
