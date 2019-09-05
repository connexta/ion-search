/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.exceptions;

import com.connexta.search.common.exceptions.DetailedResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * This exception means the indexing service was unable to successful complete a request to make
 * data searchable.
 */
public class IndexException extends DetailedResponseStatusException {

  public IndexException(HttpStatus status, String reason, Throwable cause) {
    super(status, reason, cause);
  }

  public IndexException(HttpStatus status, String reason) {
    super(status, reason);
  }
}
