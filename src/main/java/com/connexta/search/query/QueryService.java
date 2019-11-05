/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.query.exceptions.QueryException;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public interface QueryService {

  /**
   * @throws SearchException if there was an error querying or the cql was invalid
   * @throws QueryException if unable to construct IRM retrieve URI
   */
  @NotNull
  List<URI> find(
      @NotNull @Pattern(regexp = ".*\\S.*") @Size(min = 1, max = 100) final String cqlString);
}
