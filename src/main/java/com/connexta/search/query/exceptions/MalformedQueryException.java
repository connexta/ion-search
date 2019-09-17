/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.exceptions;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** This exception means the query service was unable to parse the query. */
public class MalformedQueryException extends QueryException {

  public MalformedQueryException(Throwable cause) {
    super(BAD_REQUEST, "Invalid CommonQL query string", cause);
  }
}
