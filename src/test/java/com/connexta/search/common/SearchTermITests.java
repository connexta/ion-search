/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static com.connexta.search.common.configs.SolrConfiguration.*;
import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.connexta.search.index.Index;
import com.connexta.search.index.IndexManager;
import com.connexta.search.query.QueryManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

/**
 * The purpose of this test class is to verify that all search terms can be added to index without
 * causing an exception and that all search terms can be a part of a valid query sent to the index.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Testcontainers
@DirtiesContext
public class SearchTermITests {

  //  private static FakeValuesService faker = new FakeValuesService(
  //      Locale.US, new RandomService());

  private static final int SOLR_PORT = 8983;
  private static final String INDEX_ENDPOINT_BASE_URL = "/index/";

  @Container
  public static final GenericContainer solrContainer =
      new GenericContainer("solr:8.1.1")
          .withCommand("solr-create -c " + SOLR_COLLECTION)
          .withExposedPorts(SOLR_PORT)
          // TODO: would (new URL("solr", SOLR_COLLECTION, "admin", "ping").toString() work?
          .waitingFor(Wait.forHttp("/solr/" + SOLR_COLLECTION + "/admin/ping"));

  public SearchTermITests() throws IOException {}

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
  @Inject private QueryManager queryManager;
  @Inject private IndexManager indexManager;

  private Map<String, String> sampleDocument = createSampleIndexData();

  @NotNull
  private Map<String, String> createSampleIndexData() throws IOException {

    return Map.of(
        ID_ATTRIBUTE_NAME,
        UUID.randomUUID().toString(),
        CONTENTS_ATTRIBUTE_NAME,
        "Winterfell",
        MEDIA_TYPE_ATTRIBUTE_NAME,
        "application/json");
  }

  private String getQuery() {
    return QUERY_TERMS.stream()
        .map(term -> String.format("%s = '%s'", term, sampleDocument.get(term)))
        .collect(Collectors.joining(" AND "));
  }

  InputStream getMetadata() throws IOException {
    return IOUtils.toInputStream("{ \"ext.extracted.text\" : \"Winterfell\" }", "UTF-8");
  }

  @Test
  void verifySampleDataComplete() {
    assertThat(
        "Sample document is not complete", QUERY_TERMS.equals(sampleDocument.keySet()), is(true));
  }

  @Test
  public void test() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Index document = mapper.convertValue(sampleDocument, Index.class);
    indexManager.index(document.getId(), document.getMediaType(), getMetadata());
    String q = getQuery();
    List<URI> results = queryManager.find(q);
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
            // The extension of this filename is used to get the ContentType of the file.
            return "ignored.json";
          }
        });
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set("Accept-Version", "0.2.0");
    return new HttpEntity<>(requestBody, httpHeaders);
  }
}
