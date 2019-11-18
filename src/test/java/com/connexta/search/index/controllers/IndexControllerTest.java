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
import java.net.URI;
import java.net.URISyntaxException;
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

  @Mock private IndexService mockIndexService;

  private IndexApi indexApi;
  private static final String DATASET_ID = "63c2006b-32f4-437b-8689-d673107ed5a7";
  private static final UUID DATASET_UUID = UUID.fromString(DATASET_ID);
  private IndexRequest indexRequest;

  @BeforeEach
  public void beforeEach() throws URISyntaxException {
    indexApi = new IndexController(mockIndexService, INDEX_API_VERSION);
    indexRequest =
        new IndexRequest()
            .irmLocation(
                new URI(String.format("http://store:9041/dataset/%s/irm", DATASET_ID)).toString())
            .fileLocation(
                new URI(String.format("http://store:9041/dataset/%s/file", DATASET_ID)).toString());
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
            () -> indexApi.index(acceptVersion, DATASET_UUID, indexRequest));
    assertThat(thrown.getStatus(), is(HttpStatus.NOT_IMPLEMENTED));
    verifyNoInteractions(mockIndexService);
  }

  @ParameterizedTest
  @MethodSource("exceptionsThrownByIndexService")
  void testIndexServiceThrowsThrowable(final Throwable throwable) throws Exception {
    doThrow(throwable).when(mockIndexService).index(DATASET_ID, indexRequest);

    final Throwable thrown =
        assertThrows(
            Throwable.class, () -> indexApi.index(INDEX_API_VERSION, DATASET_UUID, indexRequest));
    assertThat(
        "thrown exception is the exact same exception thrown by the IndexService",
        thrown,
        is(throwable));
  }

  @Test
  void testIndex() throws Exception {
    final String uuidString = "63c2006b-32f4-437b-8689-d673107ed5a7";
    final URI fileUri = new URI(String.format("http://store:9041/dataset/%s/file", uuidString));
    final URI irmUri = new URI(String.format("http://store:9041/dataset/%s/irm", uuidString));
    final IndexRequest indexRequest =
        new IndexRequest().fileLocation(fileUri.toString()).irmLocation(irmUri.toString());
    final UUID uuid = UUID.fromString(uuidString);
    indexApi.index(INDEX_API_VERSION, uuid, indexRequest);

    verify(mockIndexService).index(uuidString, indexRequest);
  }

  private static Stream<Arguments> exceptionsThrownByIndexService() {
    return Stream.of(
        Arguments.of(
            new SearchException(HttpStatus.INTERNAL_SERVER_ERROR, "test", new Throwable("test"))),
        Arguments.of(new RuntimeException()));
  }
}
