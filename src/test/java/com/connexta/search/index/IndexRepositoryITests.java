/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Testcontainers
@DirtiesContext
class IndexRepositoryITests {

  private static final int SOLR_PORT = 8983;
  private static final String SOLR_COLLECTION = "search_terms";

  private static final String INDEX_ID = UUID.randomUUID().toString().replace("-", "");
  private static final String INDEX_CONTENT = "Winterfell";
  private static final String INDEX_MEDIA_TYPE = MediaType.APPLICATION_JSON;

  @Container
  private static final GenericContainer solrContainer =
      new GenericContainer("cnxta/search-solr")
          .withExposedPorts(SOLR_PORT)
          .waitingFor(Wait.forHttp("/solr/" + SOLR_COLLECTION + "/admin/ping"));

  @Inject private IndexRepository indexRepository;

  @Rule public ExpectedException exception = ExpectedException.none();

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
  void cleanup() {
    indexRepository.deleteAll();
  }

  @Test
  void testIndexCrud() {
    // setup
    Index index = new Index(INDEX_ID, INDEX_CONTENT, INDEX_MEDIA_TYPE);

    // when
    indexRepository.save(index);

    // then
    assertEquals(1, indexRepository.count());
    Index savedIndex = indexRepository.findById(INDEX_ID).get();
    assertEquals(index, savedIndex);

    // and
    Index updatedIndex = new Index(INDEX_ID, "updatedContext", "updated/contentType");

    // when
    indexRepository.save(updatedIndex);

    // then
    assertEquals(1, indexRepository.count());
    assertEquals(updatedIndex, indexRepository.findById(INDEX_ID).get());

    // when
    indexRepository.deleteById(INDEX_ID);

    // then
    assertEquals(0, indexRepository.count());
  }

  @Test
  void testIdRequired() {
    // setup
    Index index = new Index(null, INDEX_CONTENT, INDEX_MEDIA_TYPE);

    // when
    DataAccessResourceFailureException e =
        assertThrows(DataAccessResourceFailureException.class, () -> indexRepository.save(index));

    // then
    assertTrue(e.getMessage().contains("missing mandatory uniqueKey field: id"));
  }

  @Test
  void testContentRequired() {
    // setup
    Index index = new Index(INDEX_ID, null, INDEX_MEDIA_TYPE);

    // when
    DataAccessResourceFailureException e =
        assertThrows(DataAccessResourceFailureException.class, () -> indexRepository.save(index));

    // then
    assertTrue(e.getMessage().contains("missing required field: contents"));
    assertEquals(0, indexRepository.count());
  }

  @Test
  void testMediaTypeRequired() {
    // setup
    Index index = new Index(INDEX_ID, INDEX_CONTENT, null);

    // when
    DataAccessResourceFailureException e =
        assertThrows(DataAccessResourceFailureException.class, () -> indexRepository.save(index));

    // then
    assertTrue(e.getMessage().contains("missing required field: media_type"));
    assertEquals(0, indexRepository.count());
  }
}
