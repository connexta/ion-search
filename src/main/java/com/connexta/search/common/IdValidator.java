/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

// TODO: The validation logic is common to multiple repositories. Find a way to share the code.

import javax.validation.ValidationException;

/**
 * Encapsulate common validations of an IonID. Throw a ValidationException if there is a violation.
 */
// TODO Use a proper assertion library like Truth or Hamcrest instead of if statements
public class IdValidator {

  private final String id;
  public static final String ID_REGEX = "^[0-9a-zA-Z]{32}";

  public IdValidator(String id) {
    this.id = id;
  }

  public void validate() {
    if (id == null || !id.matches(ID_REGEX)) {
      throw new ValidationException(
          String.format("Provided ID does not match regular expression %s", ID_REGEX));
    }
  }
}
