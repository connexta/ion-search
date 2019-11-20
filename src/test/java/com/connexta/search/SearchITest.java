/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search;

import static com.connexta.search.common.SearchManagerImpl.EXT_EXTRACTED_TEXT;
import static com.connexta.search.common.configs.SolrConfiguration.CONTENTS_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.connexta.search.index.IndexComponentTest;
import com.connexta.search.index.controllers.IndexController;
import com.connexta.search.query.controllers.QueryController;
import com.connexta.search.rest.models.IndexRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * This class contains tests that use {@link WebTestClient} and a Solr docker image. Beans should
 * not be injected into this class and tested directly. Nothing should be mocked in this class.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@DirtiesContext
class SearchITest {

  private static final int SOLR_PORT = 8983;

  @Container
  private static final GenericContainer solrContainer =
      new GenericContainer("cnxta/search-solr")
          .withExposedPorts(SOLR_PORT)
          .waitingFor(Wait.forHttp("/solr/" + SOLR_COLLECTION + "/admin/ping"));

  @TestConfiguration
  static class Config {

    @Bean
    URL solrUrl() throws MalformedURLException {
      return new URL(
          "http",
          solrContainer.getContainerIpAddress(),
          solrContainer.getMappedPort(SOLR_PORT),
          "/solr");
    }
  }

  @Inject
  private SolrClient solrClient; // Injected to empty solr for each test. Not tested directly.

  @Inject private WebTestClient webTestClient;

  @Value("${endpoints.index.version}")
  private String indexApiVersion;

  private static MockWebServer storeMockWebServer;

  @BeforeEach
  void beforeEach() throws IOException, SolrServerException {
    storeMockWebServer = new MockWebServer();
    storeMockWebServer.start();

    // TODO shouldn't need to clear solr every time
    solrClient.deleteByQuery(SOLR_COLLECTION, "*");
    solrClient.commit(SOLR_COLLECTION);
  }

  @AfterEach
  void afterEach() throws IOException {
    storeMockWebServer.shutdown();
  }

  @Test
  void testContextLoads() {}

  @Test
  void testIndexWhenSolrIsEmpty() throws Exception {
    // given stub store server
    final String keyword = "Winterfell";
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(
                String.format(
                    "{ \"%s\" : \"All the color had been leached from %s until only grey and white remained.\" }",
                    EXT_EXTRACTED_TEXT, keyword))
            .setResponseCode(200));
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI irmUri = storeMockWebServer.url(String.format("/dataset/%s/irm", datasetId)).uri();

    // when index
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, datasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(irmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // then verify GET request to irmUri
    final RecordedRequest getIrmRequest = storeMockWebServer.takeRequest();
    assertThat(getIrmRequest.getMethod(), is(HttpMethod.GET.name()));
    assertThat(getIrmRequest.getPath(), is(String.format("/dataset/%s/irm", datasetId)));

    // and verify query returns irmUri
    webTestClient
        .get()
        .uri(
            URLDecoder.decode(
                new URIBuilder()
                    .setPath(QueryController.URL_TEMPLATE)
                    .setParameter("q", String.format("%s LIKE '%s'", CONTENTS_ATTRIBUTE, keyword))
                    .build()
                    .toString()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(hasItem(irmUri.toString()));
  }

  @Test
  void testIndexWhenSolrIsNotEmpty() throws Exception {
    // given index an initial IRM
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{ \"%s\" : \"First IRM metadata \" }", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String firstDatasetId = "000b27ffc35d46d9ba041f663d9ccaff";
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, firstDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue(
            (new IndexRequest()
                .irmLocation(
                    storeMockWebServer
                        .url(String.format("/dataset/%s/irm", firstDatasetId))
                        .uri())))
        .exchange()
        .expectStatus()
        .isOk();
    final RecordedRequest firstGetIrmRequest = storeMockWebServer.takeRequest();
    assertThat(firstGetIrmRequest.getMethod(), is(HttpMethod.GET.name()));
    assertThat(firstGetIrmRequest.getPath(), is(String.format("/dataset/%s/irm", firstDatasetId)));

    // when index
    final String keyword = "Winterfell";
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(
                String.format(
                    "{ \"%s\" : \"All the color had been leached from %s until only grey and white remained.\" }",
                    EXT_EXTRACTED_TEXT, keyword))
            .setResponseCode(200));
    final String datasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI irmUri = storeMockWebServer.url(String.format("/dataset/%s/irm", datasetId)).uri();
    final ResponseSpec response =
        webTestClient
            .put()
            .uri(IndexController.URL_TEMPLATE, datasetId)
            .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
            .bodyValue((new IndexRequest().irmLocation(irmUri)))
            .exchange();

    // then verify status code is 200
    response.expectStatus().isOk();

    // and verify GET request to irmUri
    final RecordedRequest getIrmRequest = storeMockWebServer.takeRequest();
    assertThat(getIrmRequest.getMethod(), is(HttpMethod.GET.name()));
    assertThat(getIrmRequest.getPath(), is(String.format("/dataset/%s/irm", datasetId)));

    // and verify query returns irmUri
    webTestClient
        .get()
        .uri(
            URLDecoder.decode(
                new URIBuilder()
                    .setPath(QueryController.URL_TEMPLATE)
                    .setParameter("q", String.format("%s LIKE '%s'", CONTENTS_ATTRIBUTE, keyword))
                    .build()
                    .toString()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(hasItem(irmUri.toString()));
  }

  @Test
  @Disabled("TODO check that the dataset exists")
  void testIndexWhenDatasetIdNotFound() {}

  /** TODO Move this to {@link IndexComponentTest}. Not sure why this fails in that class. */
  @ParameterizedTest(name = "400 is returned when the datasetId is \"{0}\"")
  @ValueSource(
      strings = {"   ", "1234567890123456789012345678901234", "+0067360b70e4acfab561fe593ad3f7a"})
  void testInvalidDatasetId(final String datasetId) throws Exception {
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, datasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue(
            (new IndexRequest()
                .irmLocation(
                    new URI("http://store:9041/dataset/00067360b70e4acfab561fe593ad3f7a/irm"))))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        CONTENTS_ATTRIBUTE + " = 'first IRM metadata'",
        CONTENTS_ATTRIBUTE + " LIKE 'first'",
        "id='000b27ffc35d46d9ba041f663d9ccaff'",
        "id='000b27ffc35d46d9ba041f663d9ccaff' AND "
            + CONTENTS_ATTRIBUTE
            + " = 'first IRM metadata'"
      })
  void testQuery(final String cqlStringOnlyMatchingFirstDataset) throws Exception {
    // given index a dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{\"%s\":\"first IRM metadata\"}", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String firstDatasetId = "000b27ffc35d46d9ba041f663d9ccaff";
    final URI firstIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", firstDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, firstDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(firstIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // and index another dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{\"%s\":\"second IRM metadata\"}", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String secondDatasetId = "001ccb7241284f21a3d15cc340c6aa9c";
    final URI secondIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", secondDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, secondDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(secondIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // and index another dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{\"%s\":\"third IRM metadata\"}", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String thirdDatasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI thirdIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", thirdDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, thirdDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(thirdIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // verify query only returns firstUri
    webTestClient
        .get()
        .uri(
            URLDecoder.decode(
                new URIBuilder()
                    .setPath(QueryController.URL_TEMPLATE)
                    .setParameter("q", cqlStringOnlyMatchingFirstDataset)
                    .build()
                    .toString()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(
            Matchers.allOf(
                hasItem(firstIrmUri.toString()),
                not(hasItem(secondIrmUri.toString())),
                not(hasItem(thirdIrmUri.toString()))));
  }

  @Test
  void testMultipleQueryResults() throws Exception {
    // given
    final String keyword = "matchHere";

    // and index a dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(
                String.format("{\"%s\":\"first IRM metadata %s\"}", EXT_EXTRACTED_TEXT, keyword))
            .setResponseCode(200));
    final String firstDatasetId = "000b27ffc35d46d9ba041f663d9ccaff";
    final URI firstIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", firstDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, firstDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(firstIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // and index another dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(
                String.format("{\"%s\":\"second IRM metadata %s\"}", EXT_EXTRACTED_TEXT, keyword))
            .setResponseCode(200));
    final String secondDatasetId = "001ccb7241284f21a3d15cc340c6aa9c";
    final URI secondIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", secondDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, secondDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(secondIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // and index another dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{\"%s\":\"third IRM metadata\"}", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String thirdDatasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI thirdIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", thirdDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, thirdDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(thirdIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // verify query only returns firstUri
    webTestClient
        .get()
        .uri(
            URLDecoder.decode(
                new URIBuilder()
                    .setPath(QueryController.URL_TEMPLATE)
                    .setParameter("q", CONTENTS_ATTRIBUTE + " LIKE '" + keyword + "'")
                    .build()
                    .toString()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(
            Matchers.allOf(
                hasItem(firstIrmUri.toString()),
                hasItem(secondIrmUri.toString()),
                not(hasItem(thirdIrmUri.toString()))));
  }

  @Test
  void testQueryZeroSearchResults() throws Exception {
    // given index a dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{\"%s\":\"first IRM metadata\"}", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String firstDatasetId = "000b27ffc35d46d9ba041f663d9ccaff";
    final URI firstIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", firstDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, firstDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(firstIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // and index another dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{\"%s\":\"second IRM metadata\"}", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String secondDatasetId = "001ccb7241284f21a3d15cc340c6aa9c";
    final URI secondIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", secondDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, secondDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(secondIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // and index another dataset
    storeMockWebServer.enqueue(
        new MockResponse()
            .setBody(String.format("{\"%s\":\"third IRM metadata\"}", EXT_EXTRACTED_TEXT))
            .setResponseCode(200));
    final String thirdDatasetId = "00067360b70e4acfab561fe593ad3f7a";
    final URI thirdIrmUri =
        storeMockWebServer.url(String.format("/dataset/%s/irm", thirdDatasetId)).uri();
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, thirdDatasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest().irmLocation(thirdIrmUri)))
        .exchange()
        .expectStatus()
        .isOk();

    // verify query that doesn't match any of the datasets does not return the irmUris for those
    // datasets
    webTestClient
        .get()
        .uri(
            URLDecoder.decode(
                new URIBuilder()
                    .setPath(QueryController.URL_TEMPLATE)
                    .setParameter("q", CONTENTS_ATTRIBUTE + " LIKE 'this doesn''t match any IRM'")
                    .build()
                    .toString()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(
            Matchers.allOf(
                not(hasItem(firstIrmUri.toString())),
                not(hasItem(secondIrmUri.toString())),
                not(hasItem(thirdIrmUri.toString()))));
  }

  @Test
  void testQueryWhenSolrIsEmpty() throws Exception {
    webTestClient
        .get()
        .uri(
            URLDecoder.decode(
                new URIBuilder()
                    .setPath(QueryController.URL_TEMPLATE)
                    .setParameter(
                        "q",
                        String.format(
                            "%s LIKE 'nothing is in solr so this won''t match anything'",
                            CONTENTS_ATTRIBUTE))
                    .build()
                    .toString()))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(isEmpty());
  }

  /**
   * TODO Move this to {@link com.connexta.search.query.QueryServiceComponentTest}. Not sure why
   * this fails in that class.
   */
  @ParameterizedTest(name = "400 Bad Request is returned when query uri is {0}")
  @MethodSource("badQueryStrings")
  void testBadQueryRequests(final String query) {
    webTestClient
        .get()
        .uri(
            UriComponentsBuilder.fromPath(QueryController.URL_TEMPLATE)
                .query(QueryController.QUERY_URL_TEMPLATE)
                .build()
                .toString(),
            query)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  private static Stream<Arguments> badQueryStrings() {
    return Stream.of(
        Arguments.of(""),
        Arguments.of("city LIKE 'Paradise City'"),
        Arguments.of("contents SORTALIKE 'metadata'"),
        Arguments.of("x".repeat(5001)));
  }

  @NotNull
  private static TypeSafeMatcher<List> hasItem(final String string) {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(final List list) {
        return list.contains(string);
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("a List containing " + string);
      }
    };
  }

  @NotNull
  private static TypeSafeMatcher<List> isEmpty() {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(final List list) {
        return list.isEmpty();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("is empty");
      }
    };
  }
}
