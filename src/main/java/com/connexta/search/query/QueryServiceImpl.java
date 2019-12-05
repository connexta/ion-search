/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import java.net.URI;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class QueryServiceImpl implements QueryService {

  @NotNull private final QueryStorageAdaptor queryStorageAdaptor;

  @Override
  public Set<URI> find(final String commonQL) {
    return queryStorageAdaptor.query(commonQL);
  }
}
