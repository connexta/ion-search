/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import com.connexta.search.common.exceptions.SearchException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Interfaces with the index provider. */
public interface SearchManager {

  /**
   * Indexes the resource.
   *
   * @param datasetId the id of the dataset
   * @param irmUri {@link URI} to download the IRM of the dataset
   * @param irmInputStream the irm
   * @throws SearchException if there was an error indexing the resource
   */
  void index(
      @Pattern(regexp = "^[0-9a-zA-Z]+$") @Size(min = 32, max = 32) final String datasetId,
      @NotNull final URI irmUri,
      @NotNull final InputStream irmInputStream);

  /**
   * Query the index provider with the given CQL string.
   *
   * @param cql the index query
   * @return a {@link Set} of IRM {@link URI}s
   * @throws SearchException if there was an error querying or the cql was invalid
   */
  Set<URI> query(String cql);
}
