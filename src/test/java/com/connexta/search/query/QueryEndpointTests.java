/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.search.query.controllers.QueryController;
import com.connexta.search.query.exceptions.IllegalQueryException;
import com.connexta.search.query.exceptions.MalformedQueryException;
import com.connexta.search.query.exceptions.QueryException;
import java.util.ArrayList;
import java.util.Arrays;
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

  public static final String QUERY_STRING = "id=12efab35fab21afdd8932afa38951aef";
  public static final String URI_QUERY_PARAMETER = "q";
  public static final String SEARCH_ENDPOINT = "/search";

  @MockBean private QueryManager queryManager;

  @Inject private QueryController queryController;

  @Inject private MockMvc mockMvc;

  private static Stream<Arguments> requestsThatThrowErrors() {
    return Stream.of(
        Arguments.of(500, new QueryException(HttpStatus.INTERNAL_SERVER_ERROR, "Test")),
        Arguments.of(400, new IllegalQueryException(Arrays.asList("Test"))),
        Arguments.of(400, new MalformedQueryException(new RuntimeException())));
  }

  @Test
  public void testQueryManagerReturnsList() throws Exception {
    given(queryManager.find(QUERY_STRING)).willReturn(new ArrayList<>());

    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SEARCH_ENDPOINT);
    uriBuilder.setParameter(URI_QUERY_PARAMETER, QUERY_STRING);
    mockMvc.perform(MockMvcRequestBuilders.get(uriBuilder.build())).andExpect(status().isOk());
  }

  @ParameterizedTest(name = "{0} is returned when QueryManager#find throws {1}")
  @MethodSource("requestsThatThrowErrors")
  public void testExceptionHandling(int responseStatus, Throwable throwableType) throws Exception {
    given(queryManager.find(QUERY_STRING)).willThrow(throwableType);

    final URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setPath(SEARCH_ENDPOINT);
    uriBuilder.setParameter(URI_QUERY_PARAMETER, QUERY_STRING);
    mockMvc
        .perform(MockMvcRequestBuilders.get(uriBuilder.build()))
        .andExpect(status().is(responseStatus));
  }
}
