/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.exceptions;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import javax.validation.constraints.NotNull;

/** This exception means the query service was unable to parse the query. */
public class MalformedQueryException extends QueryException {

  public MalformedQueryException(@NotNull final Throwable cause) {
    super(BAD_REQUEST, "Invalid CommonQL query string", cause);
  }
}
