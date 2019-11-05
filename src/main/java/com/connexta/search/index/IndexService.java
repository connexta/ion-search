/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import com.connexta.search.index.exceptions.IndexException;
import java.io.InputStream;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** Interfaces with the index provider. */
public interface IndexService {

  /**
   * Indexes the resource.
   *
   * @param datasetId the id of the resource
   * @param mediaType the type of the resource
   * @param inputStream the resource
   * @throws IndexException if there was an error indexing the resource
   */
  void index(
      @NotBlank String datasetId, @NotBlank String mediaType, @NotNull InputStream inputStream)
      throws IndexException;

  /**
   * Query the index provider with the given CQL string.
   *
   * @param cql the index query
   * @return a set of entry IDs
   * @throws IndexException if there was an error querying the index provider or the cql was invalid
   */
  Set<String> query(String cql) throws IndexException;
}
