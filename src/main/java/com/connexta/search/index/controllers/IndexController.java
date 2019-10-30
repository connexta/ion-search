/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.controllers;

import com.connexta.search.index.IndexManager;
import com.connexta.search.index.exceptions.IndexException;
import com.connexta.search.rest.spring.IndexApi;
import java.io.IOException;
import java.io.InputStream;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@AllArgsConstructor
public class IndexController implements IndexApi {

  public static final String ACCEPT_VERSION_HEADER_NAME = "Accept-Version";

  @NotNull private final IndexManager indexManager;
  @NotBlank private final String indexApiVersion;

  @Override
  public ResponseEntity<Void> index(
      final String acceptVersion, final String datasetId, final MultipartFile file) {
    final String expectedAcceptVersion = indexApiVersion;
    if (!StringUtils.equals(acceptVersion, expectedAcceptVersion)) {
      log.warn(
          "Expected "
              + ACCEPT_VERSION_HEADER_NAME
              + " to be \"{}\" but was \"{}\". Only \"{}\" is currently supported.",
          expectedAcceptVersion,
          acceptVersion,
          expectedAcceptVersion);
      return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    // TODO validate datasetId

    final String mediaType = file.getContentType();

    // TODO handle when CST has already been stored

    final InputStream inputStream;
    try {
      inputStream = file.getInputStream();
    } catch (IOException e) {
      throw new IndexException(
          HttpStatus.BAD_REQUEST,
          String.format(
              "Unable to read file for index request with params acceptVersion=%s, datasetId=%s, mediaType=%s",
              acceptVersion, datasetId, mediaType),
          e);
    }
    indexManager.index(datasetId, mediaType, inputStream);
    return ResponseEntity.ok().build();
  }
}
