/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.exceptions;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.connexta.search.common.exceptions.DetailedResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * This exception means the query service was unable to successfully fulfill a request. This is also
 * the base class of more specialized query exceptions.
 */
public class QueryException extends DetailedResponseStatusException {

  public QueryException(HttpStatus status, String reason, Throwable cause) {
    super(status, reason, cause);
  }

  public QueryException(String reason, Throwable cause) {
    this(INTERNAL_SERVER_ERROR, reason, cause);
  }
}
