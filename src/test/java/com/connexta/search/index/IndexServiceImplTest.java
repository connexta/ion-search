/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static com.connexta.search.common.SearchManagerImpl.EXT_EXTRACTED_TEXT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.connexta.search.common.SearchManager;
import com.connexta.search.common.exceptions.SearchException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class IndexServiceImplTest {

  @Mock private SearchManager mockSearchManager;

  @AfterEach
  public void afterEach() {
    verifyNoMoreInteractions(mockSearchManager);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 500, 501})
  void testIrmUriReturnsNotOkStatusCode(final int code) throws Exception {
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";

    final SearchException thrown;
    try (final MockWebServer storeMockWebServer = new MockWebServer()) {
      storeMockWebServer.start();
      storeMockWebServer.enqueue(new MockResponse().setResponseCode(code));

      thrown =
          assertThrows(
              SearchException.class,
              () ->
                  ((IndexService) new IndexServiceImpl(mockSearchManager, WebClient.create()))
                      .index(
                          datasetId,
                          storeMockWebServer
                              .url(String.format("/dataset/%s/irm", datasetId))
                              .uri()));
    }
    assertThat(thrown.getStatus(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  void testWebClientException(
      @Mock final WebClient mockWebClient,
      @Mock final RequestHeadersUriSpec mockRequestHeadersUriSpec,
      @Mock final RequestHeadersSpec mockRequestHeadersSpec,
      @Mock final ResponseSpec mockResponseSpec,
      @Mock final ResponseSpec mockResponseSpec2,
      @Mock final Mono<Resource> mockMono)
      throws Exception {
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI irmUri = new URI(String.format("http://store:9041/dataset/%s/irm", datasetId));

    when(mockWebClient.get()).thenReturn(mockRequestHeadersUriSpec);
    when(mockRequestHeadersUriSpec.uri(irmUri)).thenReturn(mockRequestHeadersSpec);
    when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.onStatus(any(Predicate.class), any(Function.class)))
        .thenReturn(mockResponseSpec2);
    when(mockResponseSpec2.bodyToMono(Resource.class)).thenReturn(mockMono);

    final RuntimeException runtimeException = new RuntimeException();
    when(mockMono.block()).thenThrow(runtimeException);

    final SearchException thrown =
        assertThrows(
            SearchException.class,
            () ->
                ((IndexService) new IndexServiceImpl(mockSearchManager, mockWebClient))
                    .index(datasetId, irmUri));
    assertThat(thrown.getStatus(), is(HttpStatus.BAD_REQUEST));
    assertThat(thrown.getCause(), is(runtimeException));
  }

  @Test
  void testNullResource(
      @Mock final WebClient mockWebClient,
      @Mock final RequestHeadersUriSpec mockRequestHeadersUriSpec,
      @Mock final RequestHeadersSpec mockRequestHeadersSpec,
      @Mock final ResponseSpec mockResponseSpec,
      @Mock final ResponseSpec mockResponseSpec2,
      @Mock final Mono<Resource> mockMono)
      throws Exception {
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI irmUri = new URI(String.format("http://store:9041/dataset/%s/irm", datasetId));

    when(mockWebClient.get()).thenReturn(mockRequestHeadersUriSpec);
    when(mockRequestHeadersUriSpec.uri(irmUri)).thenReturn(mockRequestHeadersSpec);
    when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.onStatus(any(Predicate.class), any(Function.class)))
        .thenReturn(mockResponseSpec2);
    when(mockResponseSpec2.bodyToMono(Resource.class)).thenReturn(mockMono);
    when(mockMono.block()).thenReturn(null);

    final SearchException thrown =
        assertThrows(
            SearchException.class,
            () ->
                ((IndexService) new IndexServiceImpl(mockSearchManager, mockWebClient))
                    .index(datasetId, irmUri));
    assertThat(thrown.getStatus(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  void testCouldNotOpenInputStream(
      @Mock final WebClient mockWebClient,
      @Mock final RequestHeadersUriSpec mockRequestHeadersUriSpec,
      @Mock final RequestHeadersSpec mockRequestHeadersSpec,
      @Mock final ResponseSpec mockResponseSpec,
      @Mock final ResponseSpec mockResponseSpec2,
      @Mock final Mono<Resource> mockMono,
      @Mock final Resource mockResource)
      throws Exception {
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI irmUri = new URI(String.format("http://store:9041/dataset/%s/irm", datasetId));

    when(mockWebClient.get()).thenReturn(mockRequestHeadersUriSpec);
    when(mockRequestHeadersUriSpec.uri(irmUri)).thenReturn(mockRequestHeadersSpec);
    when(mockRequestHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.onStatus(any(Predicate.class), any(Function.class)))
        .thenReturn(mockResponseSpec2);
    when(mockResponseSpec2.bodyToMono(Resource.class)).thenReturn(mockMono);
    when(mockMono.block()).thenReturn(mockResource);

    final IOException ioException = new IOException();
    when(mockResource.getInputStream()).thenThrow(ioException);

    final SearchException thrown =
        assertThrows(
            SearchException.class,
            () ->
                ((IndexService) new IndexServiceImpl(mockSearchManager, mockWebClient))
                    .index(datasetId, irmUri));
    assertThat(thrown.getStatus(), is(HttpStatus.BAD_REQUEST));
    assertThat(thrown.getCause(), is(ioException));
  }

  @ParameterizedTest
  @MethodSource("exceptionsThrownSearchManager")
  void testSearchManagerExceptions(final RuntimeException runtimeException) throws Exception {
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";

    final RuntimeException thrown;

    try (final MockWebServer storeMockWebServer = new MockWebServer()) {
      storeMockWebServer.start();
      storeMockWebServer.enqueue(
          new MockResponse()
              .setBody(
                  String.format(
                      "{ \"%s\" : \"All the color had been leached from Winterfell until only grey and white remained.\" }",
                      EXT_EXTRACTED_TEXT))
              .setResponseCode(200));
      final URI irmUri = storeMockWebServer.url(String.format("/dataset/%s/irm", datasetId)).uri();
      doThrow(runtimeException)
          .when(mockSearchManager)
          .index(eq(datasetId), eq(irmUri), any(InputStream.class));

      thrown =
          assertThrows(
              RuntimeException.class,
              () ->
                  ((IndexService) new IndexServiceImpl(mockSearchManager, WebClient.create()))
                      .index(datasetId, irmUri));
    }

    assertThat(
        "thrown exception is the exact same exception thrown by the SearchManager",
        thrown,
        is(runtimeException));
  }

  @Test
  void testIndex() throws Exception {
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final String body =
        String.format(
            "{ \"%s\" : \"All the color had been leached from Winterfell until only grey and white remained.\" }",
            EXT_EXTRACTED_TEXT);

    final URI irmUri;
    try (final MockWebServer storeMockWebServer = new MockWebServer()) {
      storeMockWebServer.start();
      storeMockWebServer.enqueue(new MockResponse().setBody(body).setResponseCode(200));
      irmUri = storeMockWebServer.url(String.format("/dataset/%s/irm", datasetId)).uri();

      ((IndexService) new IndexServiceImpl(mockSearchManager, WebClient.create()))
          .index(datasetId, irmUri);
    }

    final ArgumentCaptor<InputStream> inputStreamCaptor =
        ArgumentCaptor.forClass(InputStream.class);
    verify(mockSearchManager).index(eq(datasetId), eq(irmUri), inputStreamCaptor.capture());
    assertThat(IOUtils.toString(inputStreamCaptor.getValue(), StandardCharsets.UTF_8), is(body));
  }

  private static Stream<Arguments> exceptionsThrownSearchManager() {
    return Stream.of(
        Arguments.of(
            new SearchException(HttpStatus.INTERNAL_SERVER_ERROR, "test", new Throwable("test"))),
        Arguments.of(new RuntimeException()));
  }
}
