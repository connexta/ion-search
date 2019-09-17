/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.connexta.search.query.exceptions.IllegalQueryException;
import com.connexta.search.query.exceptions.MalformedQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommonQlTest {

  @Test
  public void testValidQueries() {
    assertNotNull(QueryManagerImpl.getFilter("contents LIKE 'bloop'"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"XXX LIKE 'bloop'", "contents LIKE 'Winterfell' OR XXX LIKE 'Kings Landing'"})
  public void testUnsupportedAttributes(String queryString) {
    try {
      QueryManagerImpl.getFilter(queryString);
    } catch (IllegalQueryException e) {
      assertThat(e.getReason(), containsString("XXX"));
      return;
    }
    fail("Expected an exception");
  }

  @Test
  public void testBadQueryGrammar() {

    assertThrows(
        MalformedQueryException.class, () -> QueryManagerImpl.getFilter("contents LIKE don't"));
  }
}
