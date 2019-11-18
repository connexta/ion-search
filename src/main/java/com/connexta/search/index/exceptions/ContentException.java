/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.exceptions;

public class ContentException extends Exception {

  public ContentException(String message) {
    super(message);
  }

  public ContentException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
