/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search;

import static com.connexta.search.common.configs.SolrConfiguration.CONTENTS_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.connexta.search.common.SearchManager;
import com.connexta.search.index.controllers.IndexController;
import com.connexta.search.query.QueryService;
import com.connexta.search.query.controllers.QueryController;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Testcontainers
@DirtiesContext
class SearchITest {

  private static final int SOLR_PORT = 8983;

  private static final String IRM_PATH_SEGMENT = "irm";

  @Container
  private static final GenericContainer solrContainer =
      new GenericContainer("cnxta/search-solr")
          .withExposedPorts(SOLR_PORT)
          .waitingFor(Wait.forHttp("/solr/" + SOLR_COLLECTION + "/admin/ping"));

  private static final String INDEX_ENDPOINT_BASE_URL = "/index/";

  @Inject private QueryService queryService;
  @Inject private SearchManager searchManager;
  @Inject private TestRestTemplate restTemplate;
  @Inject private SolrClient solrClient;

  @Value("${endpointUrl.datasetRetrieve}")
  private String datasetRetrieveEndpoint;

  @Value("${endpoints.index.version}")
  private String indexApiVersion;

  @TestConfiguration
  static class Config {

    @Bean
    public URL solrUrl() throws MalformedURLException {
      return new URL(
          "http",
          solrContainer.getContainerIpAddress(),
          solrContainer.getMappedPort(SOLR_PORT),
          "/solr");
    }
  }

  @AfterEach
  public void afterEach() throws IOException, SolrServerException {
    // TODO shouldn't need to clear solr every time
    solrClient.deleteByQuery(SOLR_COLLECTION, "*");
    solrClient.commit(SOLR_COLLECTION);
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "text"})
  public void testStoringValidCst(String contents) throws Exception {
    // given
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";

    // when indexing IRM
    ResponseEntity<String> response =
        restTemplate.exchange(
            indexEndpointUrl,
            HttpMethod.PUT,
            createIndexRequest("{ \"ext.extracted.text\" : \"" + contents + "\" }"),
            String.class);

    // then
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{}", "{ \"\": \"text\"}"})
  public void testStoringInvalidCst(String contents) throws IOException {
    // given
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";
    // when indexing IRM
    ResponseEntity<String> response =
        restTemplate.exchange(
            indexEndpointUrl, HttpMethod.PUT, createIndexRequest(contents), String.class);

    // then
    assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void testStoreMetadataCstWhenSolrIsEmpty() throws Exception {
    // given
    final String queryKeyword = "Winterfell";
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";
    final String irmLocation =
        String.format(
            "%s/%s/%s",
            datasetRetrieveEndpoint, "00067360b70e4acfab561fe593ad3f7a", IRM_PATH_SEGMENT);

    // when indexing IRM
    restTemplate.put(
        indexEndpointUrl,
        createIndexRequest(
            "{ \"ext.extracted.text\" : \""
                + ("All the color had been leached from "
                    + queryKeyword
                    + " until only grey and white remained")
                + " \" }"));

    // then
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath(QueryController.URL_TEMPLATE);
    queryUriBuilder.setParameter(
        "q", String.format("%s LIKE '%s'", CONTENTS_ATTRIBUTE, queryKeyword));
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(irmLocation));
  }

  @Test
  void testStoreMetadataCstWhenSolrIsNotEmpty() throws Exception {
    // given index an initial IRM
    restTemplate.put(
        (INDEX_ENDPOINT_BASE_URL + "000b27ffc35d46d9ba041f663d9ccaff"),
        createIndexRequest("{ \"ext.extracted.text\" : \"" + ("First IRM metadata") + " \" }"));

    // and create the index request for another IRM
    final String queryKeyword = "Winterfell";
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";
    final String irmLocation =
        String.format(
            "%s/%s/%s",
            datasetRetrieveEndpoint, "00067360b70e4acfab561fe593ad3f7a", IRM_PATH_SEGMENT);

    // when indexing another IRM
    restTemplate.put(
        indexEndpointUrl,
        createIndexRequest(
            "{ \"ext.extracted.text\" : \""
                + ("All the color had been leached from "
                    + queryKeyword
                    + " until only grey and white remained")
                + " \" }"));

    // then
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath(QueryController.URL_TEMPLATE);
    queryUriBuilder.setParameter(
        "q", String.format("%s LIKE '%s'", CONTENTS_ATTRIBUTE, queryKeyword));
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(equalTo(irmLocation)));
  }

  @Test
  @Disabled("TODO check that the dataset exists before storing cst")
  void testStoreMetadataDatasetIdNotFound() {}

  @Test
  void testStoreWhenDatasetHasAlreadyBeenIndexed() throws Exception {
    // given index IRM
    final String queryKeyword = "Winterfell";
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";
    final String irmLocation =
        String.format(
            "%s/%s/%s",
            datasetRetrieveEndpoint, "00067360b70e4acfab561fe593ad3f7a", IRM_PATH_SEGMENT);

    restTemplate.put(
        indexEndpointUrl,
        createIndexRequest(
            "{ \"ext.extracted.text\" : \""
                + ("All the color had been leached from "
                    + queryKeyword
                    + " until only grey and white remained")
                + " \" }"));

    // when indexing it again (override or same file doesn't matter)
    // TODO fix status code returned here
    restTemplate.put(
        indexEndpointUrl,
        createIndexRequest("{\"ext.extracted.text\":\"new \"ext.extracted.text\"\"}"));

    // then query should still work
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath(QueryController.URL_TEMPLATE);
    queryUriBuilder.setParameter(
        "q", String.format("%s LIKE '%s'", CONTENTS_ATTRIBUTE, queryKeyword));
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(irmLocation));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        CONTENTS_ATTRIBUTE + " = 'first IRM metadata'",
        CONTENTS_ATTRIBUTE + " LIKE 'first'",
        "id='000b27ffc35d46d9ba041f663d9ccaff'"
      })
  void testQueryMultipleResults(final String cqlString) throws Exception {
    // given IRM is indexed
    final String firstId = "000b27ffc35d46d9ba041f663d9ccaff";
    final String firstIndexUrl = INDEX_ENDPOINT_BASE_URL + firstId;
    final String firstLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, firstId, IRM_PATH_SEGMENT);
    final String firstIndexContents = "{\"ext.extracted.text\":\"first IRM metadata\"}";
    restTemplate.put(firstIndexUrl, createIndexRequest(firstIndexContents));

    // and another IRM is indexed
    final String secondId = "001ccb7241284f21a3d15cc340c6aa9c";
    final String secondIndexUrl = INDEX_ENDPOINT_BASE_URL + secondId;
    final String secondLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, secondId, IRM_PATH_SEGMENT);
    restTemplate.put(
        secondIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"second IRM metadata\"}"));

    // and another IRM is indexed
    final String thirdId = "00067360b70e4acfab561fe593ad3f7a";
    final String thirdIndexUrl = INDEX_ENDPOINT_BASE_URL + thirdId;
    final String thirdLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, thirdId, IRM_PATH_SEGMENT);
    restTemplate.put(
        thirdIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"third IRM metadata\"}"));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath(QueryController.URL_TEMPLATE);
    queryUriBuilder.setParameter("q", cqlString);
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        allOf(hasItem(firstLocation), not(hasItem(secondLocation)), not(hasItem(thirdLocation))));
  }

  // TODO test multiple results
  @Test
  void testQueryWhenSolrIsNotEmpty() throws Exception {
    // given IRM is indexed
    final String firstId = "000b27ffc35d46d9ba041f663d9ccaff";
    final String firstIndexUrl = INDEX_ENDPOINT_BASE_URL + firstId;
    final String firstLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, firstId, IRM_PATH_SEGMENT);
    final String firstIrmKeyword = "first";
    restTemplate.put(
        firstIndexUrl,
        createIndexRequest("{\"ext.extracted.text\":\"" + firstIrmKeyword + " IRM metadata\"}"));

    // and another IRM is indexed
    final String secondId = "001ccb7241284f21a3d15cc340c6aa9c";
    final String secondIndexUrl = INDEX_ENDPOINT_BASE_URL + secondId;
    final String secondLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, secondId, IRM_PATH_SEGMENT);
    restTemplate.put(
        secondIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"second IRM metadata\"}"));

    // and another IRM is indexed
    final String thirdId = "00067360b70e4acfab561fe593ad3f7a";
    final String thirdIndexUrl = INDEX_ENDPOINT_BASE_URL + thirdId;
    final String thirdLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, thirdId, IRM_PATH_SEGMENT);
    restTemplate.put(
        thirdIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"third IRM metadata\"}"));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath(QueryController.URL_TEMPLATE);
    queryUriBuilder.setParameter(
        "q", String.format("%s LIKE '%s'", CONTENTS_ATTRIBUTE, firstIrmKeyword));
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(firstLocation));
  }

  @Test
  void testQueryZeroSearchResults() throws Exception {
    // given IRM is indexed
    final String firstId = "000b27ffc35d46d9ba041f663d9ccaff";
    final String firstIndexUrl = INDEX_ENDPOINT_BASE_URL + firstId;
    final String firstLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, firstId, IRM_PATH_SEGMENT);
    final String firstIrmKeyword = "first";
    restTemplate.put(
        firstIndexUrl,
        createIndexRequest("{\"ext.extracted.text\":\"" + firstIrmKeyword + " IRM metadata\"}"));

    // and another IRM is indexed
    final String secondId = "001ccb7241284f21a3d15cc340c6aa9c";
    final String secondIndexUrl = INDEX_ENDPOINT_BASE_URL + secondId;
    final String secondLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, secondId, IRM_PATH_SEGMENT);
    restTemplate.put(
        secondIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"second IRM metadata\"}"));

    // and another IRM is indexed
    final String thirdId = "00067360b70e4acfab561fe593ad3f7a";
    final String thirdIndexUrl = INDEX_ENDPOINT_BASE_URL + thirdId;
    final String thirdLocation =
        String.format("%s/%s/%s", datasetRetrieveEndpoint, thirdId, IRM_PATH_SEGMENT);
    restTemplate.put(
        thirdIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"third IRM metadata\"}"));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath(QueryController.URL_TEMPLATE);
    queryUriBuilder.setParameter(
        "q", String.format("%s LIKE 'this doesn''t match any IRM'", CONTENTS_ATTRIBUTE));
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        allOf(
            not(hasItem(firstLocation)),
            not(hasItem(secondLocation)),
            not(hasItem(thirdLocation))));
  }

  @Test
  @Disabled("TODO")
  void testMultipleSearchResults() throws Exception {
    // TODO
  }

  @Test
  void testQueryWhenSolrIsEmpty() throws Exception {
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath(QueryController.URL_TEMPLATE);
    queryUriBuilder.setParameter(
        "q",
        String.format(
            "%s LIKE 'nothing is in solr so this wont match anything'", CONTENTS_ATTRIBUTE));
    assertThat(
        (List<URI>) restTemplate.getForObject(queryUriBuilder.build(), List.class), is(empty()));
  }

  private HttpEntity createIndexRequest(final String requestContent) throws IOException {
    // TODO replace with request class from api dependency
    final InputStream metadataInputStream = IOUtils.toInputStream(requestContent, "UTF-8");
    final MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
    requestBody.add(
        "file",
        new InputStreamResource(metadataInputStream) {

          @Override
          public long contentLength() throws IOException {
            return metadataInputStream.available();
          }

          @Override
          public String getFilename() {
            // The extension of this filename is used to get the ContentType of the file.
            return "ignored.json";
          }
        });
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion);
    return new HttpEntity<>(requestBody, httpHeaders);
  }
}
