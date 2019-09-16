/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;

import com.connexta.search.query.exceptions.IllegalQueryException;
import com.connexta.search.query.exceptions.MalformedQueryException;
import org.junit.jupiter.api.Test;

class CommonQlTest {

  public static final String INVALID_ATTRIBUTE_NAME = "xxxxxxxxxxxxx";
  public static final String QUERY_UNSUPPORTED_ATTRIBUTE = INVALID_ATTRIBUTE_NAME + " LIKE 'bloop'";
  public static final String QUERY_VALID = "contents LIKE 'bloop'";
  public static final String QUERY_UNMATCHED_DELIM = "contents LIKE don't";
  public static final String QUERY_BOOLEAN_UNSUPPORTED_ATTRIBUTE =
      "contents LIKE 'Winterfell' OR " + INVALID_ATTRIBUTE_NAME + " LIKE 'Kings Landing'";

  @Test
  public void testValidQuery() {
    CommonQl object = new CommonQl(QUERY_VALID);
    object.validate();
    assertNotNull(object.getFilter());
  }

  @Test
  public void testIllegalSearchAttribute() {
    assertThrows(IllegalQueryException.class, new CommonQl(QUERY_UNSUPPORTED_ATTRIBUTE)::validate);
  }

  @Test
  public void testIllegalSearchAttirubteWithoutValidation() {
    assertNotNull(new CommonQl(QUERY_UNSUPPORTED_ATTRIBUTE).getFilter());
  }

  @Test
  public void testBadQueryGrammar() {
    CommonQl object = new CommonQl(QUERY_UNMATCHED_DELIM);
    assertThrows(MalformedQueryException.class, object::getFilter);
    assertThrows(MalformedQueryException.class, object::validate);
    assertThrows(MalformedQueryException.class, object::extractUnsupportedAttributes);
  }

  @Test
  public void testBooleanQueryWithIllegalSearchAttribute() {

    assertThrows(
        IllegalQueryException.class, new CommonQl(QUERY_BOOLEAN_UNSUPPORTED_ATTRIBUTE)::validate);
  }

  @Test
  public void testFindingUnsupportedAttributes() {
    assertThat(
        new CommonQl(QUERY_UNSUPPORTED_ATTRIBUTE).extractUnsupportedAttributes(),
        hasItem(INVALID_ATTRIBUTE_NAME));
  }
}
