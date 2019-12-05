/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static com.connexta.search.common.configs.SolrConfiguration.CONTENTS_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.FILE_URL_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.ID_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.IRM_URL_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.METACARD_URL_ATTRIBUTE;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.connexta.search.common.configs.SolrConfiguration;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
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
class IndexStorageAdaptorITest {

  private static final int SOLR_PORT = 8983;

  private static final String ID = UUID.randomUUID().toString();
  private static final String CONTENTS = "Winterfell";
  private static final String IRM_URL = "irmLocation";
  private static final String METACARD_URL = "metacardLocation";
  private static final String FILE_URL = "fileLocation";
  private static final String MISSING_REQUIRED_FIELD_MESSAGE_FORMAT = "missing required field: %s";

  @Container
  private static final GenericContainer solrContainer =
      new GenericContainer("cnxta/search-solr")
          .withExposedPorts(SOLR_PORT)
          .waitingFor(Wait.forHttp("/solr/" + SolrConfiguration.SOLR_COLLECTION + "/admin/ping"));

  private static final String INDEX_COUNTRY = "USA";
  private static final Date INDEX_CREATED = new Date(1574179891);
  private static final Date INDEX_MODIFIED = new Date(1000088888);

  @Inject private IndexStorageAdaptor indexStorageAdaptor;

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
    indexStorageAdaptor.deleteAll();
  }

  @Test
  void testIndex() {
    // setup
    Index index = baseInstance();

    // when
    indexStorageAdaptor.save(index);

    // then
    assertThat(indexStorageAdaptor.count(), is(1L));
    assertThat(indexStorageAdaptor.findById(ID), isPresentAndIs(index));
  }

  @Test
  void testDelete() {
    // setup
    Index index = baseInstance();
    indexStorageAdaptor.save(index);

    // when
    indexStorageAdaptor.deleteById(ID);

    // then
    assertThat(indexStorageAdaptor.count(), is(0L));
  }

  @Test
  void testIdRequired() {
    // setup
    Index index =
        Index.builder()
            .contents(CONTENTS)
            .irmUrl(IRM_URL)
            .metacardUrl(METACARD_URL)
            .fileUrl(FILE_URL)
            .build();

    // when
    DataAccessResourceFailureException e =
        assertThrows(
            DataAccessResourceFailureException.class, () -> indexStorageAdaptor.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(String.format("missing mandatory uniqueKey field: %s", ID_ATTRIBUTE)));
    assertThat(indexStorageAdaptor.count(), is(0L));
  }

  @Test
  void testContentsRequired() {
    // setup
    Index index =
        Index.builder().id(ID).irmUrl(IRM_URL).metacardUrl(METACARD_URL).fileUrl(FILE_URL).build();

    // when
    DataAccessResourceFailureException e =
        assertThrows(
            DataAccessResourceFailureException.class, () -> indexStorageAdaptor.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(String.format(MISSING_REQUIRED_FIELD_MESSAGE_FORMAT, CONTENTS_ATTRIBUTE)));
    assertThat(indexStorageAdaptor.count(), is(0L));
  }

  @Test
  void testIrmUrlRequired() {
    // setup
    Index index =
        Index.builder()
            .id(ID)
            .contents(CONTENTS)
            .metacardUrl(METACARD_URL)
            .fileUrl(FILE_URL)
            .build();

    // when
    DataAccessResourceFailureException e =
        assertThrows(
            DataAccessResourceFailureException.class, () -> indexStorageAdaptor.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(String.format(MISSING_REQUIRED_FIELD_MESSAGE_FORMAT, IRM_URL_ATTRIBUTE)));
    assertThat(indexStorageAdaptor.count(), is(0L));
  }

  @Test
  void testFileUrlRequired() {
    // setup
    Index index =
        Index.builder().id(ID).contents(CONTENTS).irmUrl(IRM_URL).metacardUrl(METACARD_URL).build();

    // when
    DataAccessResourceFailureException e =
        assertThrows(
            DataAccessResourceFailureException.class, () -> indexStorageAdaptor.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(String.format(MISSING_REQUIRED_FIELD_MESSAGE_FORMAT, FILE_URL_ATTRIBUTE)));
    assertThat(indexStorageAdaptor.count(), is(0L));
  }

  @Test
  void testMetacardUrlRequired() {
    // setup
    Index index =
        Index.builder().id(ID).contents(CONTENTS).irmUrl(IRM_URL).fileUrl(FILE_URL).build();

    // when
    DataAccessResourceFailureException e =
        assertThrows(
            DataAccessResourceFailureException.class, () -> indexStorageAdaptor.save(index));

    // then
    assertThat(
        e.getMessage(),
        containsString(
            String.format(MISSING_REQUIRED_FIELD_MESSAGE_FORMAT, METACARD_URL_ATTRIBUTE)));
    assertThat(indexStorageAdaptor.count(), is(0L));
  }

  @Test
  @Disabled(
      "TODO: Test fails. Enable test when we learn how to make Solr reject the second save action")
  void testUpdateProhibited() {
    final Index index = baseInstance();
    indexStorageAdaptor.save(index);
    indexStorageAdaptor.findById(ID).get();
    Index updatedIndex =
        Index.builder()
            .id(ID)
            .contents("Updated")
            .countryCode(INDEX_COUNTRY)
            .created(INDEX_CREATED)
            .irmUrl(IRM_URL)
            .modified(INDEX_MODIFIED)
            .build();
    indexStorageAdaptor.save(updatedIndex);
    Index indexAfterSave = indexStorageAdaptor.findById(ID).get();
    assertThat(index, equalTo(indexAfterSave));
  }

  private Index baseInstance() {
    return Index.builder()
        .id(ID)
        .contents(CONTENTS)
        .countryCode(INDEX_COUNTRY)
        .created(INDEX_CREATED)
        .irmUrl(IRM_URL)
        .fileUrl(FILE_URL)
        .metacardUrl(METACARD_URL)
        .modified(INDEX_MODIFIED)
        .build();
  }
}
