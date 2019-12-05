/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.index.IndexService;
import com.connexta.search.rest.models.IndexRequest;
import com.connexta.search.rest.spring.IndexApi;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
public class IndexControllerTest {

  private static final String INDEX_API_VERSION = "testIndexApiVersion";
  private static final UUID DATASET_ID = UUID.randomUUID();
  private static final IndexRequest INDEX_REQUEST =
      new IndexRequest()
          .irmLocation(String.format("http://store:9041/dataset/%s/irm", DATASET_ID.toString()))
          .fileLocation(String.format("http://store:9041/dataset/%s/file", DATASET_ID.toString()))
          .metacardLocation(
              String.format("http://store:9041/dataset/%s/metacard", DATASET_ID.toString()));

  @Mock private IndexService mockIndexService;

  private IndexApi indexApi;

  @BeforeEach
  public void beforeEach() {
    indexApi = new IndexController(mockIndexService, INDEX_API_VERSION);
  }

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(mockIndexService);
  }

  @ParameterizedTest(name = "ValidationException when acceptVersion is {0}")
  @NullAndEmptySource
  @ValueSource(strings = {"this is invalid"})
  void testInvalidAcceptVersion(final String acceptVersion) {
    final ResponseStatusException thrown =
        assertThrows(
            ResponseStatusException.class,
            () -> indexApi.index(acceptVersion, DATASET_ID, INDEX_REQUEST));
    assertThat(thrown.getStatus(), is(HttpStatus.NOT_IMPLEMENTED));
    verifyNoInteractions(mockIndexService);
  }

  @ParameterizedTest
  @MethodSource("exceptionsThrownByIndexService")
  void testIndexServiceThrowsThrowable(final Throwable throwable) {
    doThrow(throwable).when(mockIndexService).index(DATASET_ID, INDEX_REQUEST);

    final Throwable thrown =
        assertThrows(
            Throwable.class, () -> indexApi.index(INDEX_API_VERSION, DATASET_ID, INDEX_REQUEST));
    assertThat(
        "thrown exception is the exact same exception thrown by the IndexService",
        thrown,
        is(throwable));
  }

  @Test
  void testIndex() {
    indexApi.index(INDEX_API_VERSION, DATASET_ID, INDEX_REQUEST);

    verify(mockIndexService).index(DATASET_ID, INDEX_REQUEST);
  }

  private static Stream<Arguments> exceptionsThrownByIndexService() {
    return Stream.of(
        Arguments.of(
            new SearchException(HttpStatus.INTERNAL_SERVER_ERROR, "test", new Throwable("test"))),
        Arguments.of(new RuntimeException()));
  }
}
