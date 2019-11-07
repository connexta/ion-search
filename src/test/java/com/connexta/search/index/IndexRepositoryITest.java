/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static com.connexta.search.common.configs.SolrConfiguration.CONTENTS_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.ID_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.MEDIA_TYPE_ATTRIBUTE;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.connexta.search.common.Index;
import com.connexta.search.common.IndexRepository;
import com.connexta.search.common.configs.SolrConfiguration;
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

/**
 * TODO Update this to a unit test or use {@link
 * org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest}/{@link
 * org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest} instead.
 */
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Testcontainers
@DirtiesContext
class IndexRepositoryITest {

  private static final int SOLR_PORT = 8983;

  private static final String INDEX_ID = UUID.randomUUID().toString().replace("-", "");
  private static final String INDEX_CONTENT = "Winterfell";
  private static final String INDEX_MEDIA_TYPE = MediaType.APPLICATION_JSON;
  private static final String MISSING_REQUIRED_FIELD_MESSAGE_FORMAT = "missing required field: %s";

  @Container
  private static final GenericContainer solrContainer =
      new GenericContainer("cnxta/search-solr")
          .withExposedPorts(SOLR_PORT)
          .waitingFor(Wait.forHttp("/solr/" + SolrConfiguration.SOLR_COLLECTION + "/admin/ping"));

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
  void testIndex() {
    // setup
    Index index = new Index(INDEX_ID, INDEX_CONTENT, INDEX_MEDIA_TYPE);

    // when
    indexRepository.save(index);

    // then
    assertThat(indexRepository.count(), is(1L));
    assertThat(indexRepository.findById(INDEX_ID), isPresentAndIs(index));
  }

  @Test
  void testUpdate() {
    // setup
    Index index = new Index(INDEX_ID, INDEX_CONTENT, INDEX_MEDIA_TYPE);
    indexRepository.save(index);

    Index updatedIndex = new Index(INDEX_ID, "updatedContext", "updated/contentType");

    // when
    indexRepository.save(updatedIndex);

    // then
    assertThat(indexRepository.count(), is(1L));
    assertThat(indexRepository.findById(INDEX_ID), isPresentAndIs(updatedIndex));
  }

  @Test
  void testDelete() {
    // setup
    Index index = new Index(INDEX_ID, INDEX_CONTENT, INDEX_MEDIA_TYPE);
    indexRepository.save(index);

    // when
    indexRepository.deleteById(INDEX_ID);

    // then
    assertThat(indexRepository.count(), is(0L));
  }

  @Test
  void testIdRequired() {
    // setup
    Index index = new Index(null, INDEX_CONTENT, INDEX_MEDIA_TYPE);

    // when
    DataAccessResourceFailureException e =
        assertThrows(DataAccessResourceFailureException.class, () -> indexRepository.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(String.format("missing mandatory uniqueKey field: %s", ID_ATTRIBUTE)));
    assertThat(indexRepository.count(), is(0L));
  }

  @Test
  void testContentRequired() {
    // setup
    Index index = new Index(INDEX_ID, null, INDEX_MEDIA_TYPE);

    // when
    DataAccessResourceFailureException e =
        assertThrows(DataAccessResourceFailureException.class, () -> indexRepository.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(String.format(MISSING_REQUIRED_FIELD_MESSAGE_FORMAT, CONTENTS_ATTRIBUTE)));
    assertThat(indexRepository.count(), is(0L));
  }

  @Test
  void testMediaTypeRequired() {
    // setup
    Index index = new Index(INDEX_ID, INDEX_CONTENT, null);

    // when
    DataAccessResourceFailureException e =
        assertThrows(DataAccessResourceFailureException.class, () -> indexRepository.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(String.format(MISSING_REQUIRED_FIELD_MESSAGE_FORMAT, MEDIA_TYPE_ATTRIBUTE)));
    assertThat(indexRepository.count(), is(0L));
  }
}
