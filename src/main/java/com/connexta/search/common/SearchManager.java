/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import com.connexta.search.common.exceptions.SearchException;
import java.net.URI;
import java.util.Set;

/** Interfaces with the index provider. */
public interface SearchManager {

  /**
   * Query the index provider with the given CQL string.
   *
   * @param cql the index query
   * @return a {@link Set} of IRM {@link URI}s
   * @throws SearchException if there was an error querying or the cql was invalid
   */
  Set<URI> query(String cql);
}
