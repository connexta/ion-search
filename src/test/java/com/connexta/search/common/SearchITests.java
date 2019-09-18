/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.connexta.search.common.configs.SolrConfiguration;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Testcontainers
@DirtiesContext
public class SearchITests {

  private static final int SOLR_PORT = 8983;
  private static final String INDEX_ENDPOINT_BASE_URL = "/mis/index/";

  @Container
  public static final GenericContainer solrContainer =
      new GenericContainer("solr:8.1.1")
          .withCommand("solr-create -c " + SOLR_COLLECTION)
          .withExposedPorts(SOLR_PORT)
          .waitingFor(Wait.forHttp("/solr/" + SOLR_COLLECTION + "/admin/ping"));

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

  @Inject private TestRestTemplate restTemplate;
  @Inject private SolrClient solrClient;

  @Value("${endpointUrl.retrieve}")
  private String retrieveEndpoint;

  @BeforeEach
  public void beforeEach() throws IOException, SolrServerException {
    // TODO shouldn't need to clear solr every time
    solrClient.deleteByQuery(SOLR_COLLECTION, "*");
    solrClient.commit(SOLR_COLLECTION);
  }

  @Test
  public void testContextLoads() {}

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "text"})
  public void testStoringValidCst(String contents) throws Exception {
    // given
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";

    // when indexing a product
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
    // when indexing a product
    ResponseEntity<String> response =
        restTemplate.exchange(
            indexEndpointUrl, HttpMethod.PUT, createIndexRequest(contents), String.class);

    // then
    assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testStoreMetadataCstWhenSolrIsEmpty() throws Exception {
    // given
    final String queryKeyword = "Winterfell";
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";
    final String productLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";

    // when indexing a product
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
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter(
        "q", SolrConfiguration.CONTENTS_ATTRIBUTE_NAME + " LIKE '" + queryKeyword + "'");
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(productLocation));
  }

  @Test
  public void testStoreMetadataCstWhenSolrIsNotEmpty() throws Exception {
    // given index an initial product
    restTemplate.put(
        (INDEX_ENDPOINT_BASE_URL + "000b27ffc35d46d9ba041f663d9ccaff"),
        createIndexRequest("{ \"ext.extracted.text\" : \"" + ("First product metadata") + " \" }"));

    // and create the index request for another product
    final String queryKeyword = "Winterfell";
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";
    final String productLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";

    // when indexing another product
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
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter(
        "q", SolrConfiguration.CONTENTS_ATTRIBUTE_NAME + " LIKE '" + queryKeyword + "'");
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(equalTo(productLocation)));
  }

  @Test
  @Disabled("TODO check that the product exists before storing cst")
  public void testStoreMetadataProductIdNotFound() {}

  @Test
  public void testStoreWhenProductHasAlreadyBeenIndexed() throws Exception {
    // given index a product
    final String queryKeyword = "Winterfell";
    final String indexEndpointUrl = INDEX_ENDPOINT_BASE_URL + "00067360b70e4acfab561fe593ad3f7a";
    final String productLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";

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
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter(
        "q", SolrConfiguration.CONTENTS_ATTRIBUTE_NAME + " LIKE '" + queryKeyword + "'");
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(productLocation));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        SolrConfiguration.CONTENTS_ATTRIBUTE_NAME + " = 'first product metadata'",
        SolrConfiguration.CONTENTS_ATTRIBUTE_NAME + " LIKE 'first'",
        "id='000b27ffc35d46d9ba041f663d9ccaff'"
      })
  public void testQueryMultipleResults(final String cqlString) throws Exception {
    // given a product is indexed
    final String firstId = "000b27ffc35d46d9ba041f663d9ccaff";
    final String firstIndexUrl = INDEX_ENDPOINT_BASE_URL + firstId;
    final String firstLocation = retrieveEndpoint + firstId;
    final String firstIndexContents = "{\"ext.extracted.text\":\"first product metadata\"}";
    restTemplate.put(firstIndexUrl, createIndexRequest(firstIndexContents));

    // and another product is indexed
    final String secondId = "001ccb7241284f21a3d15cc340c6aa9c";
    final String secondIndexUrl = INDEX_ENDPOINT_BASE_URL + secondId;
    final String secondLocation = retrieveEndpoint + secondId;
    restTemplate.put(
        secondIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"second product metadata\"}"));

    // and another product is indexed
    final String thirdId = "00067360b70e4acfab561fe593ad3f7a";
    final String thirdIndexUrl = INDEX_ENDPOINT_BASE_URL + thirdId;
    final String thirdLocation = retrieveEndpoint + thirdId;
    restTemplate.put(
        thirdIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"third product metadata\"}"));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", cqlString);
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        allOf(hasItem(firstLocation), not(hasItem(secondLocation)), not(hasItem(thirdLocation))));
  }

  // TODO test multiple results
  @Test
  public void testQueryWhenSolrIsNotEmpty() throws Exception {
    // given a product is indexed
    final String firstId = "000b27ffc35d46d9ba041f663d9ccaff";
    final String firstIndexUrl = INDEX_ENDPOINT_BASE_URL + firstId;
    final String firstLocation = retrieveEndpoint + firstId;
    final String firstProductKeyword = "first";
    restTemplate.put(
        firstIndexUrl,
        createIndexRequest(
            "{\"ext.extracted.text\":\"" + firstProductKeyword + " product metadata\"}"));

    // and another product is indexed
    final String secondId = "001ccb7241284f21a3d15cc340c6aa9c";
    final String secondIndexUrl = INDEX_ENDPOINT_BASE_URL + secondId;
    final String secondLocation = retrieveEndpoint + secondId;
    restTemplate.put(
        secondIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"second product metadata\"}"));

    // and another product is indexed
    final String thirdId = "00067360b70e4acfab561fe593ad3f7a";
    final String thirdIndexUrl = INDEX_ENDPOINT_BASE_URL + thirdId;
    final String thirdLocation = retrieveEndpoint + thirdId;
    restTemplate.put(
        thirdIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"third product metadata\"}"));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter(
        "q", SolrConfiguration.CONTENTS_ATTRIBUTE_NAME + " LIKE '" + firstProductKeyword + "'");
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(firstLocation));
  }

  @Test
  public void testQueryZeroSearchResults() throws Exception {
    // given a product is indexed
    final String firstId = "000b27ffc35d46d9ba041f663d9ccaff";
    final String firstIndexUrl = INDEX_ENDPOINT_BASE_URL + firstId;
    final String firstLocation = retrieveEndpoint + firstId;
    final String firstProductKeyword = "first";
    restTemplate.put(
        firstIndexUrl,
        createIndexRequest(
            "{\"ext.extracted.text\":\"" + firstProductKeyword + " product metadata\"}"));

    // and another product is indexed
    final String secondId = "001ccb7241284f21a3d15cc340c6aa9c";
    final String secondIndexUrl = INDEX_ENDPOINT_BASE_URL + secondId;
    final String secondLocation = retrieveEndpoint + secondId;
    restTemplate.put(
        secondIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"second product metadata\"}"));

    // and another product is indexed
    final String thirdId = "00067360b70e4acfab561fe593ad3f7a";
    final String thirdIndexUrl = INDEX_ENDPOINT_BASE_URL + thirdId;
    final String thirdLocation = retrieveEndpoint + thirdId;
    restTemplate.put(
        thirdIndexUrl, createIndexRequest("{\"ext.extracted.text\":\"third product metadata\"}"));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter(
        "q", SolrConfiguration.CONTENTS_ATTRIBUTE_NAME + " LIKE 'this doesn''t match any product'");
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        allOf(
            not(hasItem(firstLocation)),
            not(hasItem(secondLocation)),
            not(hasItem(thirdLocation))));
  }

  @Test
  @Disabled("TODO")
  public void testMultipleSearchResults() throws Exception {
    // TODO
  }

  @Test
  public void testQueryWhenSolrIsEmpty() throws Exception {
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter(
        "q",
        SolrConfiguration.CONTENTS_ATTRIBUTE_NAME
            + " LIKE 'nothing is in solr so this wont match anything'");
    assertThat(
        (List<URI>) restTemplate.getForObject(queryUriBuilder.build(), List.class), is(empty()));
  }

  private static HttpEntity createIndexRequest(final String fileString) throws IOException {
    // TODO replace with request class from api dependency
    final InputStream metadataInputStream = IOUtils.toInputStream(fileString, "UTF-8");
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
            return "test_file.json";
          }
        });
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set("Accept-Version", "0.1.0");
    return new HttpEntity<>(requestBody, httpHeaders);
  }
}
