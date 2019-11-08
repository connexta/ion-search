/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import com.connexta.search.common.SearchManager;
import com.connexta.search.common.exceptions.SearchException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
public class IndexServiceImpl implements IndexService {

  @NotNull private final SearchManager searchManager;
  @NotNull private final WebClient webClient;

  @Override
  public void index(
      @Pattern(regexp = "^[0-9a-zA-Z]+$") @Size(min = 32, max = 32) final String datasetId,
      @NotNull final URI irmUri) {
    final Resource resource;
    try {
      final HttpStatus expectedHttpStatus = HttpStatus.OK;
      resource =
          webClient
              .get()
              .uri(irmUri)
              .retrieve()
              .onStatus(
                  httpStatus -> !expectedHttpStatus.equals(httpStatus),
                  clientResponse ->
                      Mono.error(
                          () ->
                              new SearchException(
                                  BAD_REQUEST,
                                  String.format(
                                      "Excepted %s but received status code of %s when getting resource at irmUri=%s",
                                      expectedHttpStatus, clientResponse.statusCode(), irmUri))))
              .bodyToMono(Resource.class)
              .block();
    } catch (final Exception e) {
      throw new SearchException(
          BAD_REQUEST, String.format("Unable to complete GET request to irmUri=%s", irmUri), e);
    }

    if (resource == null) {
      throw new SearchException(
          BAD_REQUEST,
          String.format(
              "Unable to complete GET request to irmUri=%s because the resource was null", irmUri));
    }

    try (final InputStream irmInputStream = resource.getInputStream()) {
      searchManager.index(datasetId, irmUri, irmInputStream);
    } catch (IOException e) {
      throw new SearchException(
          BAD_REQUEST, String.format("Unable to get InputStream from irmUri=%s", irmUri), e);
    }
  }
}
