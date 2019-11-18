/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search;

import static com.connexta.search.common.configs.SolrConfiguration.CONTENTS_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;
import static com.connexta.search.common.configs.SolrConfiguration.TITLE_ATTRIBUTE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.connexta.search.index.IndexComponentTest;
import com.connexta.search.index.controllers.IndexController;
import com.connexta.search.query.controllers.QueryController;
import com.connexta.search.rest.models.IndexRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

  public static final UUID DATASET_ID = UUID.fromString("b69921fa-858f-4648-9d89-f05bd66231a7");

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

  /* TODO Because Solr is empty at the beginning of every test, every test that indexes a dataset is also testing "index when solr is empty. There is an opportunity to reduce test code without reducing coverage. */
  @Test
  void testIndexWhenSolrIsEmpty() throws Exception {
    // given stub store server
    final String keyword = "Winterfell";
    IndexRequest indexRequest = setupStoreRetrieveEndpoint(DATASET_ID, keyword, "contents");

    /* TODO: Move verification of Indexing service to location test class more focused on indexing? then verify GET request to irmUri */
    final RecordedRequest getIrmRequest = storeMockWebServer.takeRequest();
    assertThat(getIrmRequest.getMethod(), is(HttpMethod.GET.name()));
    assertThat(getIrmRequest.getPath(), is(String.format("/dataset/%s/irm", DATASET_ID)));

    // and verify query returns irmUri
    final String query = String.format("%s LIKE '%s'", TITLE_ATTRIBUTE, keyword);
    sendQuery(query)
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(hasItem(indexRequest.getIrmLocation()));
  }

  /* TODO: This tests adding multiple datsets to the the index when Solr is empty. Because Solr is empty at the beginning of every test, any test that indexes more than one dataset is also testing "index when solr not empty. There is an opportunity to reduce test code without reducing coverage. */
  // This also tests adding two datasets to the index and sending a query that should only match one
  // of them.
  @Test
  void testIndexWhenSolrIsNotEmpty() throws Exception {
    // given index an initial IRM
    final String keyword = "ABC";
    final String query = TITLE_ATTRIBUTE + " LIKE '" + keyword + "'";
    IndexRequest indexRequestFind =
        setupStoreRetrieveEndpoint(UUID.fromString("e1db1eb2-a20c-437e-804d-1d71aab6e81a"), keyword, "content");
    IndexRequest indexRequestNoFind =
        setupStoreRetrieveEndpoint(UUID.fromString("3ae92cfc-219c-4f50-89f9-cb224bb82bc0"), "123", "content");

    // verify query returns correct result
    sendQuery(query)
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(hasItem(indexRequestFind.getIrmLocation()));
  }

  @Test
  void testIndexWhenDatasetIdNotFound() {}

  /** TODO Move this to {@link IndexComponentTest}. Not sure why this fails in that class. */
  /* TODO: Could invalid IDs be tested in a unit test? Or a test that didn't spin up the Spring env and Solr? */
  @ParameterizedTest(name = "400 is returned when the datasetId is \"{0}\"")
  @ValueSource(
      strings = {"   ", "1234567890123456789012345678901234", "+0067360b70e4acfab561fe593ad3f7a"})
  void testInvalidDatasetId(final String datasetId) throws Exception {
    indexDataset(
            datasetId,
            new IndexRequest()
                .fileLocation("http://mockdoesntcare")
                .irmLocation("http://mockdoesntcare"))
        .expectStatus()
        .isBadRequest();
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        TITLE_ATTRIBUTE + " = 'first IRM metadata'",
        TITLE_ATTRIBUTE + " LIKE 'first'",
        CONTENTS_ATTRIBUTE + " = 'contents'",
          TITLE_ATTRIBUTE + " = 'first IRM metadata' AND " + CONTENTS_ATTRIBUTE + " = 'contents'"
      })
  void testQuery(final String cqlStringOnlyMatchingFirstDataset) throws Exception {
    // given index datasets
    final UUID firstDatasetId = DATASET_ID;
    final UUID secondDatasetId = UUID.fromString("e1db1eb2-a20c-437e-804d-1d71aab6e81a");
    final UUID thirdDatasetId = UUID.fromString("3ae92cfc-219c-4f50-89f9-cb224bb82bc0");
    final IndexRequest indexRequest1 =
        setupStoreRetrieveEndpoint(firstDatasetId, "first IRM metadata", "contents");
    final IndexRequest indexRequest2 =
        setupStoreRetrieveEndpoint(secondDatasetId, "second IRM metadata", "contents");
    final IndexRequest indexRequest3 =
        setupStoreRetrieveEndpoint(thirdDatasetId, "third IRM metadata", "contents");

    // verify query only returns firstUri
    sendQuery(cqlStringOnlyMatchingFirstDataset)
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(
            Matchers.allOf(
                hasItem(indexRequest1.getIrmLocation()),
                not(hasItem(indexRequest2.getIrmLocation())),
                not(hasItem(indexRequest3.getIrmLocation()))));
  }

  @Test
  void testMultipleQueryResults() throws Exception {
    // given
    // Stage two datasets (each has a File and an IRM resource) in the Store Retrieve endpoint.
    // Then call the Index endpoint to index them.

    // TODO: The tempIrmContents will become an IRM document in the future.
    final String tempIrmContents = "irm";
    final String query = TITLE_ATTRIBUTE + " LIKE '" + tempIrmContents + "'";

    List<IndexRequest> indexRequests = new ArrayList<>();
    List<String> expectedIrmUris = new ArrayList<>();
    for (UUID id :
        List.of(DATASET_ID, (UUID.fromString("e1db1eb2-a20c-437e-804d-1d71aab6e81a")))) {
      final IndexRequest indexRequest = setupStoreRetrieveEndpoint(id, tempIrmContents, "contents");
      indexRequests.add(indexRequest);
      expectedIrmUris.add(indexRequest.getIrmLocation());
    }

    // Hit the Search Query endpoint and search for term common to both IRM documents. Expect the
    // results to contain the URLs to the two IRM documents.
    sendQuery(query)
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(this.containsInAnyOrder(expectedIrmUris));
  }

  @Test
  void testQueryZeroSearchResults() throws Exception {
    // given index a dataset
    IndexRequest indexRequest = setupStoreRetrieveEndpoint(DATASET_ID, "irm", "contents");

    // verify query that doesn't match any of the datasets does not return the irmUris for those
    // datasets
    sendQuery(CONTENTS_ATTRIBUTE + " LIKE 'this doesn''t match any IRM'")
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(this.isEmpty());
  }

  @Test
  void testQueryWhenSolrIsEmpty() throws Exception {
    sendQuery(
            String.format(
                "%s LIKE 'nothing is in solr so this won''t match anything'", CONTENTS_ATTRIBUTE))
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

  @Test
  void testQueryingExtractedText() throws URISyntaxException {
    IndexRequest request = setupStoreRetrieveEndpoint(DATASET_ID, "irm", "content");
    final String query = String.format("%s LIKE '%s'", CONTENTS_ATTRIBUTE, "content");

    sendQuery(query)
        .expectStatus()
        .isOk()
        .expectBody(List.class)
        .value(hasItem(request.getIrmLocation()));
  }

  private ResponseSpec indexDataset(String datasetId, IndexRequest indexRequest) {
    // Assumes the Store Retrieve endpoint is setup to respond with the contents of the File and
    // IRM.
    return webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, datasetId)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue(indexRequest)
        .exchange();
  }

  @NotNull
  private ResponseSpec sendQuery(String query) throws URISyntaxException {
    return webTestClient
        .get()
        .uri(
            URLDecoder.decode(
                new URIBuilder()
                    .setPath(QueryController.URL_TEMPLATE)
                    .setParameter("q", query)
                    .build()
                    .toString()))
        .exchange();
  }

  /* Given a collection of dataset IDs, setup a mock Store Retrieve endpoint to return, first an
  IRM document for a GET request and then a File's contents for EACH dataset ID. Return a collection
  of index request objects that can be sent to the Index endpoint.
   */
  @NotNull
  private IndexRequest setupStoreRetrieveEndpoint(
      UUID datasetId, String irmContent, String fileContent) {
    IndexRequest indexRequest =
        new IndexRequest()
            .irmLocation(
                storeMockWebServer
                    .url(String.format("/dataset/%s/irm", datasetId))
                    .uri()
                    .toString())
            .fileLocation(storeMockWebServer.url("/mockserverdoesntcare").uri().toString());


    //TODO: Eventually  incorporate metacard into tests
    indexRequest.setMetacardLocation("http://none");

    // Each call to the Index endpoint kicks off two calls to the Store endpoint
    storeMockWebServer.enqueue(new MockResponse().setBody(irmContent).setResponseCode(200));
    storeMockWebServer.enqueue(new MockResponse().setBody(fileContent).setResponseCode(200));
    indexDataset(datasetId.toString(), indexRequest).expectStatus().isOk();
    return indexRequest;
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
  private static TypeSafeMatcher<List> containsInAnyOrder(Collection actualItems) {

    return new TypeSafeMatcher<>() {

      @Override
      protected boolean matchesSafely(List list) {
        return CollectionUtils.isEqualCollection(list, actualItems);
      }

      @Override
      public void describeTo(final Description description) {
        description.appendValue(actualItems);
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
