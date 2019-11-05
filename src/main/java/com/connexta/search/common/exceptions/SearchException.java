/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common.exceptions;

import com.connexta.search.common.SearchManager;
import org.springframework.http.HttpStatus;

/** This exception means the {@link SearchManager} was unable to successful complete a request. */
public class SearchException extends DetailedResponseStatusException {

  public SearchException(HttpStatus status, String reason, Throwable cause) {
    super(status, reason, cause);
  }

  public SearchException(HttpStatus status, String reason) {
    super(status, reason);
  }
}
