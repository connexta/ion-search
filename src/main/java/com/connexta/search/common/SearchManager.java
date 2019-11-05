/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import com.connexta.search.common.exceptions.SearchException;
import java.io.InputStream;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Interfaces with the index provider. */
public interface SearchManager {

  /**
   * Indexes the resource.
   *
   * @param datasetId the id of the dataset
   * @param mediaType the type of the dataset
   * @param inputStream the cst {@link InputStream}
   * @throws SearchException if there was an error indexing the resource
   */
  void index(
      @Pattern(regexp = "^[0-9a-zA-Z]+$") @Size(min = 32, max = 32) final String datasetId,
      @NotBlank final String mediaType,
      @NotNull final InputStream inputStream);

  /**
   * Query the index provider with the given CQL string.
   *
   * @param cql the index query
   * @return a set of entry IDs
   * @throws SearchException if there was an error querying or the cql was invalid
   */
  Set<String> query(String cql);
}
