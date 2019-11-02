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
import java.util.List;
import javax.inject.Inject;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@AllArgsConstructor
@Validated
@Slf4j
public class QueryController implements QueryApi {

  @NotNull private final QueryManager queryManager;

  @Inject Validator validator;

  @Override
  public ResponseEntity<List<URI>> query(final String q) {

    // TODO Make a String validator: new StringValidator.notBlank.maxchars(5000).validate();
    if (q.length() > 5000) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Query string cannot be more than 5,000 characters");
    }
    return ResponseEntity.ok(queryManager.find(q));
  }
}
