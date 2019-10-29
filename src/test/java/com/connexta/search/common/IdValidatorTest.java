/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdValidatorTest {

  @ParameterizedTest(name = "400 Bad Request if ID is {0}")
  @ValueSource(
      strings = {
        "",
        "   ",
        "1234567890123456789012345678901234",
        "+0067360b70e4acfab561fe593ad3f7a"
      })
  void testBadIds(String id) {
    assertThrows(
        ValidationException.class,
        () -> {
          new IdValidator(id).validate();
        });
  }

  @Test
  void testNullId() {
    assertThrows(
        ValidationException.class,
        () -> {
          new IdValidator(null).validate();
        });
  }
}
