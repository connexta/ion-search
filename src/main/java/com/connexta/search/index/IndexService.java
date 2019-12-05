/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.rest.models.IndexRequest;
import java.util.UUID;

public interface IndexService {

  /**
   * Create a persistent record of the information in the {@link IndexRequest}. Extract searchable
   * information and create an entry for the dataset.
   *
   * @throws SearchException if unable to index
   */
  void index(final UUID datasetId, final IndexRequest indexRequest);
}
