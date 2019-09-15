/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.controllers;

import com.connexta.search.query.QueryManager;
import com.connexta.search.rest.spring.QueryApi;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Validated
@Slf4j
public class QueryController implements QueryApi {

  private final QueryManager queryManager;

  @Override
  public ResponseEntity<List<URI>> query(final String q) {
    final String urlDecodedCommonQL = java.net.URLDecoder.decode(q, StandardCharsets.UTF_8);
    return ResponseEntity.ok(queryManager.find(urlDecodedCommonQL));
  }
}
