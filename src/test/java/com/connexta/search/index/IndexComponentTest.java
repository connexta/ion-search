/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.connexta.search.common.exceptions.DetailedErrorAttributes;
import com.connexta.search.index.configs.IndexControllerConfiguration;
import com.connexta.search.index.controllers.IndexController;
import com.connexta.search.index.controllers.IndexControllerTest;
import com.connexta.search.rest.models.IndexRequest;
import java.net.URI;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * This class contains tests that use {@link
 * org.springframework.test.web.reactive.server.WebTestClient} to test the {@link IndexController}
 * that cannot be tested in {@link IndexControllerTest} and that do not interact with the {@link
 * IndexService}.
 */
@WebFluxTest({
  IndexController.class,
  IndexControllerConfiguration.class,
  DetailedErrorAttributes.class
})
public class IndexComponentTest {

  @MockBean private IndexService mockIndexService;

  @Inject private WebTestClient webTestClient;

  @Value("${endpoints.index.version}")
  private String indexApiVersion;

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(mockIndexService);
  }

  @Test
  void testMissingAcceptVersion() throws Exception {
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, "00067360b70e4acfab561fe593ad3f7a")
        .bodyValue(
            (new IndexRequest()
                .irmLocation(
                    new URI("http://store:9041/dataset/00067360b70e4acfab561fe593ad3f7a/irm"))))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void testMissingBody() {
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, "00067360b70e4acfab561fe593ad3f7a")
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void testMissingIrmUrl() {
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, "00067360b70e4acfab561fe593ad3f7a")
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue((new IndexRequest()))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void testBadContentType() {
    webTestClient
        .put()
        .uri(IndexController.URL_TEMPLATE, "00067360b70e4acfab561fe593ad3f7a")
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ParameterizedTest(name = "404 is returned when the path is {0}")
  @ValueSource(
      strings = {
        "thisIsNotTheRightPathForTheIndexEndpoint/00067360b70e4acfab561fe593ad3f7a",
        "/index/00067360b70e4acfab561fe593ad3f7a/thisIsNotTheRightPathForTheIndexEndpoint",
        "/index"
      })
  void testBadPath(final String relativePath) throws Exception {
    webTestClient
        .put()
        .uri(relativePath)
        .header(IndexController.ACCEPT_VERSION_HEADER_NAME, indexApiVersion)
        .bodyValue(
            (new IndexRequest()
                .irmLocation(
                    new URI("http://store:9041/dataset/00067360b70e4acfab561fe593ad3f7a/irm"))))
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
