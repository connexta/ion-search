/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.query.QueryService;
import com.connexta.search.query.exceptions.QueryException;
import com.connexta.search.rest.spring.QueryApi;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class QueryControllerTest {

  @Mock private QueryService mockQueryService;

  private QueryApi queryApi;

  @BeforeEach
  void beforeEach() {
    queryApi = new QueryController(mockQueryService);
  }

  @AfterEach
  void after() {
    verifyNoMoreInteractions(mockQueryService);
  }

  @ParameterizedTest
  @MethodSource("results")
  void testReturnsListFromQueryManager(final Set<URI> results) {
    when(mockQueryService.find("queryString")).thenReturn(results);

    final ResponseEntity<List<URI>> result = queryApi.query("queryString");
    assertThat(result.getStatusCode(), is(HttpStatus.OK));
    assertThat(result.getBody(), containsInAnyOrder(results.toArray()));
  }

  @ParameterizedTest
  @MethodSource("requestsThatThrowErrors")
  void testExceptionHandling(final Throwable throwable) {
    when(mockQueryService.find("queryString")).thenThrow(throwable);
    final RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> queryApi.query("queryString"));

    assertThat(thrown, is(throwable));
  }

  private static Stream<Arguments> results() throws URISyntaxException {
    return Stream.of(
        Arguments.of(Set.of()),
        Arguments.of(
            Set.of(new URI("http://store:9041/dataset/12efab35fab21afdd8932afa38951aef/irm"))),
        Arguments.of(
            Set.of(
                new URI("http://store:9041/dataset/12efab35fab21afdd8932afa38951aef/irm"),
                new URI("http://store:9041/dataset/00067360b70e4acfab561fe593ad3f7a/irm"))));
  }

  private static Stream<Arguments> requestsThatThrowErrors() {
    return Stream.of(
        Arguments.of(new SearchException(HttpStatus.INTERNAL_SERVER_ERROR, "Test")),
        Arguments.of(new QueryException(HttpStatus.INTERNAL_SERVER_ERROR, "Test")),
        Arguments.of(new RuntimeException()));
  }
}
