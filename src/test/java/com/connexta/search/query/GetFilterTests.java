/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.connexta.search.query.exceptions.IllegalQueryException;
import com.connexta.search.query.exceptions.MalformedQueryException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GetFilterTests {

  @Test
  public void testValidQueries() {
    assertNotNull(QueryServiceImpl.getFilter("contents LIKE 'bloop'"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"XXX LIKE 'bloop'", "contents LIKE 'Winterfell' OR XXX LIKE 'Kings Landing'"})
  public void testUnsupportedAttributes(String queryString) {
    final IllegalQueryException illegalQueryException =
        assertThrows(IllegalQueryException.class, () -> QueryServiceImpl.getFilter(queryString));
    assertThat(illegalQueryException.getUnsupportedAttributes(), is(Set.of("XXX")));
  }

  @Test
  public void testMalformedQuery() {
    assertThrows(
        MalformedQueryException.class, () -> QueryServiceImpl.getFilter("contents LIKE don't"));
  }
}
