/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.controllers;

import com.connexta.search.common.exceptions.DetailedResponseStatusException;
import com.connexta.search.index.IndexService;
import com.connexta.search.rest.models.IndexRequest;
import com.connexta.search.rest.spring.IndexApi;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
public class IndexController implements IndexApi {

  public static final String URL_TEMPLATE = "/index/{datasetId}";
  public static final String ACCEPT_VERSION_HEADER_NAME = "Accept-Version";

  @NotNull private final IndexService indexService;
  @NotBlank private final String indexApiVersion;

  @Override
  public ResponseEntity<Void> index(
      final String acceptVersion, final UUID datasetId, @Valid final IndexRequest indexRequest) {
    final String expectedAcceptVersion = indexApiVersion;
    if (!StringUtils.equals(acceptVersion, expectedAcceptVersion)) {
      throw new DetailedResponseStatusException(
          HttpStatus.NOT_IMPLEMENTED,
          String.format(
              "%s was %s, but only %s is currently supported.",
              ACCEPT_VERSION_HEADER_NAME, acceptVersion, expectedAcceptVersion));
    }

    if (datasetId == null) {
      throw new ValidationException("Invalid dataset id");
    }

    indexService.index(datasetId, indexRequest);
    return ResponseEntity.ok().build();
  }
}
