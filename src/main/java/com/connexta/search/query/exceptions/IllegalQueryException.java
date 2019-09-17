/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.exceptions;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.Set;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;

/**
 * This exception is thrown when a query is well-formed but attempts something the query service is
 * unable or not allowed to do. This includes querying on on attributes that do not exist or are not
 * permitted to be searched.
 */
public class IllegalQueryException extends QueryException {

  @Getter @NotEmpty private final Set<String> unsupportedAttributes;

  public IllegalQueryException(@NotEmpty final Set<String> unsupportedAttributes) {
    super(
        BAD_REQUEST,
        String.format(
            "Unsupported query attributes: {%s}", String.join(", ", unsupportedAttributes)));
    this.unsupportedAttributes = unsupportedAttributes;
  }
}
