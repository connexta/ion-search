/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.search.query.controllers.QueryController;
import com.connexta.search.query.exceptions.IllegalQueryException;
import com.connexta.search.query.exceptions.MalformedQueryException;
import com.connexta.search.query.exceptions.QueryException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(QueryController.class)
public class QueryEndpointTests {

  private static final String QUERY_STRING = "id=12efab35fab21afdd8932afa38951aef";
  private static final String URI_QUERY_PARAMETER = "q";
  private static final String SEARCH_ENDPOINT = "/search";

  @MockBean private QueryManager queryManager;

  @Inject private QueryController queryController;

  @Inject private MockMvc mockMvc;

  @Test
  // TODO add a test for non-empty results
  public void testQueryControllerReturnsListFromQueryManager() throws Exception {
    final List<URI> queryResults = List.of();
    when(queryManager.find(QUERY_STRING)).thenReturn(queryResults);

    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SEARCH_ENDPOINT);
    uriBuilder.setParameter(URI_QUERY_PARAMETER, QUERY_STRING);
    mockMvc
        .perform(MockMvcRequestBuilders.get(uriBuilder.build()))
        .andExpect(status().isOk())
        .andExpect(content().string(queryResults.toString()));
  }

  @ParameterizedTest(name = "{0} is returned when QueryManager#find throws {1}")
  @MethodSource("requestsThatThrowErrors")
  public void testExceptionHandling(HttpStatus responseStatus, Throwable throwable)
      throws Exception {
    when(queryManager.find(QUERY_STRING)).thenThrow(throwable);

    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SEARCH_ENDPOINT);
    uriBuilder.setParameter(URI_QUERY_PARAMETER, QUERY_STRING);
    mockMvc
        .perform(MockMvcRequestBuilders.get(uriBuilder.build()))
        .andExpect(status().is(responseStatus.value()));
  }

  private static Stream<Arguments> requestsThatThrowErrors() {
    return Stream.of(
        Arguments.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            new QueryException(HttpStatus.INTERNAL_SERVER_ERROR, "Test")),
        Arguments.of(HttpStatus.BAD_REQUEST, new IllegalQueryException(Set.of("Test"))),
        Arguments.of(HttpStatus.BAD_REQUEST, new MalformedQueryException(new RuntimeException())));
  }
}
