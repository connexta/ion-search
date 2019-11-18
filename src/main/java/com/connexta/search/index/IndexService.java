/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import com.connexta.search.common.exceptions.SearchException;
import com.connexta.search.rest.models.IndexRequest;

public interface IndexService {

  /**
   * Create a persistent record of the information in the index request. Extract searchable
   * information and create an entry for the dataset.
   *
   * @param datasetId
   * @param indexRequest
   * @throws SearchException
   */
  void index(String datasetId, IndexRequest indexRequest);
}
