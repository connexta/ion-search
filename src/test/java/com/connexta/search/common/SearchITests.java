/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static com.connexta.search.common.Index.SOLR_COLLECTION;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import com.connexta.search.common.configs.SolrClientConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

  @Container
  public static final GenericContainer solrContainer =
      new GenericContainer("solr:8.1.1")
          .withCommand("solr-create -c " + SOLR_COLLECTION)
          .withExposedPorts(SOLR_PORT)
          .waitingFor(Wait.forHttp("/solr/" + SOLR_COLLECTION + "/admin/ping"));

  @TestConfiguration
  static class Config {

    @Bean
    public SolrClientConfiguration testSolrClientConfiguration() {
      return new SolrClientConfiguration(
          solrContainer.getContainerIpAddress(), solrContainer.getMappedPort(SOLR_PORT));
    }
  }

  @Inject private TestRestTemplate restTemplate;
  @Inject private SolrClient solrClient;

  @Value("${endpointUrl.retrieve}")
  private String retrieveEndpoint;

  @BeforeEach
  public void beforeEach() throws IOException, SolrServerException {
    solrClient.deleteByQuery(SOLR_COLLECTION, "*");
    solrClient.commit(SOLR_COLLECTION);
  }

  @Test
  public void testContextLoads() {}

  @Test
  public void testStoreMetadataCstWhenSolrIsEmpty() throws Exception {
    // given
    final String queryKeyword = "Winterfell";
    final String productLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";

    // when index a product
    restTemplate.put(
        productLocation + "/cst",
        createIndexRequest(
            "{contents:\""
                + ("All the color had been leached from "
                    + queryKeyword
                    + " until only grey and white remained")
                + "\""));

    // then
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", queryKeyword);
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(productLocation));
  }

  @Test
  public void testStoreMetadataCstWhenSolrIsNotEmpty() throws Exception {
    // given index an initial product
    restTemplate.put(
        (retrieveEndpoint + "000b27ffc35d46d9ba041f663d9ccaff") + "/cst",
        createIndexRequest("{contents:\"first product metadata\""));

    // and create the index request for another product
    final String queryKeyword = "Winterfell";
    final String productLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";

    // when index another product
    restTemplate.put(
        productLocation + "/cst",
        createIndexRequest(
            "{contents:\""
                + ("All the color had been leached from "
                    + queryKeyword
                    + " until only grey and white remained")
                + "\""));

    // then
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", queryKeyword);
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(equalTo(productLocation)));
  }

  @Test
  @Disabled("TODO check that the product exists before storing cst")
  public void testStoreMetadataProductIdNotFound() {}

  @Test
  public void testProductHasAlreadyBeenIndexed() throws Exception {
    // given index a product
    final String queryKeyword = "Winterfell";
    final String productLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";

    restTemplate.put(
        productLocation + "/cst",
        createIndexRequest(
            "{contents:\"All the color had been leached from "
                + queryKeyword
                + " until only grey and white remained\""));

    // when index it again (override or same file doesn't matter)
    // TODO fix status code returned here
    restTemplate.put(productLocation + "/cst", createIndexRequest("{contents:\"new contents\""));

    // then query should still work
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", queryKeyword);
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(productLocation));
  }

  @Test
  public void testQuery() throws Exception {
    // given a product is indexed
    final String firstLocation = retrieveEndpoint + "000b27ffc35d46d9ba041f663d9ccaff";
    restTemplate.put(
        firstLocation + "/cst", createIndexRequest("{contents:\"first product metadata\""));

    // and another product is indexed
    final String secondLocation = retrieveEndpoint + "001ccb7241284f21a3d15cc340c6aa9c";
    restTemplate.put(
        secondLocation + "/cst", createIndexRequest("{contents:\"second product metadata\""));

    // and another product is indexed
    final String thirdLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";
    restTemplate.put(
        thirdLocation + "/cst", createIndexRequest("{contents:\"third product metadata\""));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", "contents");
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItems(firstLocation, secondLocation, thirdLocation));
  }

  @Test
  public void testQueryEmptySearchResults() throws Exception {
    // given a product is indexed
    final String firstLocation = retrieveEndpoint + "000b27ffc35d46d9ba041f663d9ccaff";
    restTemplate.put(
        firstLocation + "/cst", createIndexRequest("{contents:\"first product metadata\""));

    // and another product is indexed
    final String secondLocation = retrieveEndpoint + "001ccb7241284f21a3d15cc340c6aa9c";
    restTemplate.put(
        secondLocation + "/cst", createIndexRequest("{contents:\"second product metadata\""));

    // and another product is indexed
    final String thirdLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";
    restTemplate.put(
        thirdLocation + "/cst", createIndexRequest("{contents:\"third product metadata\""));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", "this keyword doesn't match any product");
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        allOf(
            not(hasItem(firstLocation)),
            not(hasItem(secondLocation)),
            not(hasItem(thirdLocation))));
  }

  @Test
  public void testQueryMultipleResults() throws Exception {
    // given a product is indexed
    final String firstLocation = retrieveEndpoint + "000b27ffc35d46d9ba041f663d9ccaff";
    final String firstProductKeyword = "first";
    restTemplate.put(
        firstLocation + "/cst",
        createIndexRequest("{contents:\"" + firstProductKeyword + " product metadata\""));

    // and another product is indexed
    final String secondLocation = retrieveEndpoint + "001ccb7241284f21a3d15cc340c6aa9c";
    restTemplate.put(
        secondLocation + "/cst", createIndexRequest("{contents:\"second product metadata\""));

    // and another product is indexed
    final String thirdLocation = retrieveEndpoint + "00067360b70e4acfab561fe593ad3f7a";
    restTemplate.put(
        thirdLocation + "/cst", createIndexRequest("{contents:\"third product metadata\""));

    // verify
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", firstProductKeyword);
    assertThat(
        (List<String>) restTemplate.getForObject(queryUriBuilder.build(), List.class),
        hasItem(firstLocation));
  }

  @Test
  public void testQueryWhenSolrIsEmpty() throws Exception {
    final URIBuilder queryUriBuilder = new URIBuilder();
    queryUriBuilder.setPath("/search");
    queryUriBuilder.setParameter("q", "nothing is in solr so this won't match anything");
    assertThat(
        (List<URI>) restTemplate.getForObject(queryUriBuilder.build(), List.class), is(empty()));
  }

  private static HttpEntity createIndexRequest(final String fileString) throws IOException {
    // TODO replace with request class from api dependency
    final InputStream thirdMetadataInputStream = IOUtils.toInputStream(fileString, "UTF-8");
    final MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
    requestBody.add(
        "file",
        new InputStreamResource(thirdMetadataInputStream) {

          @Override
          public long contentLength() throws IOException {
            return thirdMetadataInputStream.available();
          }

          @Override
          public String getFilename() {
            return "test_file_name.txt";
          }
        });
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set("Accept-Version", "0.1.0");
    return new HttpEntity<>(requestBody, httpHeaders);
  }
}
