/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import org.springframework.data.solr.repository.SolrCrudRepository;

public interface IndexRepository extends SolrCrudRepository<Index, String> {}
