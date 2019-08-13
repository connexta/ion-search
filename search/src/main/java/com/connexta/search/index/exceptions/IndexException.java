/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.exceptions;

public class IndexException extends Exception {

  public IndexException(String message, Throwable cause) {
    super(message, cause);
  }

  public IndexException(String message) {
    super(message);
  }
}
