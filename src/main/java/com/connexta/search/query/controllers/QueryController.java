/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.controllers;

import com.connexta.search.query.QueryService;
import com.connexta.search.rest.spring.QueryApi;
import java.net.URI;
import java.util.List;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;
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

  @NotNull private final QueryService queryService;

  @Override
  public ResponseEntity<List<URI>> query(final String q) {
    // TODO Make a String validator: new StringValidator.notBlank.maxchars(5000).validate();
    if (q.length() > 5000) {
      throw new ValidationException("Query string cannot be more than 5,000 characters");
    }
    return ResponseEntity.ok(queryService.find(q));
  }
}
