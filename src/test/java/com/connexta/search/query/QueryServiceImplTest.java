/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.connexta.search.common.exceptions.SearchException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class QueryServiceImplTest {

  @Mock private QueryStorageAdaptor mockQueryStorageAdaptor;

  private QueryService queryService;

  @BeforeEach
  void beforeEach() {
    queryService = new QueryServiceImpl(mockQueryStorageAdaptor);
  }

  @AfterEach
  public void afterEach() {
    verifyNoMoreInteractions(mockQueryStorageAdaptor);
  }

  @ParameterizedTest
  @MethodSource("exceptionsThrownSearchManager")
  void testSearchManagerExceptions(final RuntimeException runtimeException) {
    final String cql = "cql";
    doThrow(runtimeException).when(mockQueryStorageAdaptor).query(cql);

    final RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> queryService.find(cql));
    assertThat(
        "thrown exception is the exact same exception thrown by the QueryStorageAdaptor",
        thrown,
        is(runtimeException));
  }

  @Test
  void testIndex() {
    final String cql = "cql";
    queryService.find(cql);
    verify(mockQueryStorageAdaptor).query(cql);
  }

  private static Stream<Arguments> exceptionsThrownSearchManager() {
    return Stream.of(
        Arguments.of(
            new SearchException(HttpStatus.INTERNAL_SERVER_ERROR, "test", new Throwable("test"))),
        Arguments.of(new RuntimeException()));
  }
}
