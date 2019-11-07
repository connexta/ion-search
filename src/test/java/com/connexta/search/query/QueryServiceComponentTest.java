/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.connexta.search.common.exceptions.DetailedErrorAttributes;
import com.connexta.search.query.controllers.QueryController;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * This class contains tests that use {@link
 * org.springframework.test.web.reactive.server.WebTestClient} to test the {@link
 * com.connexta.search.query.controllers.QueryController} that cannot be tested in {@link
 * com.connexta.search.query.controllers.QueryControllerTest} and that do not interact with the
 * {@link QueryService}.
 */
@WebFluxTest({QueryController.class, DetailedErrorAttributes.class})
public class QueryServiceComponentTest {

  @MockBean private QueryService mockQueryService;

  @Inject private WebTestClient webTestClient;

  @AfterEach
  public void after() {
    verifyNoMoreInteractions(mockQueryService);
  }

  @Test
  void testMissingQueryString() {
    webTestClient.get().uri(QueryController.URL_TEMPLATE).exchange().expectStatus().isBadRequest();
  }
}
