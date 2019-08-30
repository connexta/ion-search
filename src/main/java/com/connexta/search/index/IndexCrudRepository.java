/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

import java.util.List;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.data.solr.repository.SolrCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexCrudRepository extends SolrCrudRepository<Index, String> {

  List<Index> findByContents(String keyword) throws UncategorizedSolrException;
}
