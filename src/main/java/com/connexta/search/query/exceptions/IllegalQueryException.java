/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.exceptions;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.Collection;

/**
 * This exception is thrown when a query is well-formed but attempts something the query service is
 * unable or not allowed to do. This includes querying on on attributes that do not exist or are not
 * permitted to be searched. This exception is also thrown if there is a semantic violation. For
 * examples, can also asserting a date field is true or false, or applying a geometry predicate to a
 * numerical field.
 */
public class IllegalQueryException extends QueryException {

  public IllegalQueryException(Collection<String> unsupportedAttributes) {
    super(
        BAD_REQUEST,
        String.format(
            "Unsupported query attributes: {%s}", String.join(", ", unsupportedAttributes)));
  }
}
