/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.configs;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.query.QueryStorageAdaptor;
import com.connexta.search.query.SolrQueryStorageAdaptor;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryStorageAdaptorConfiguration {

  public static final Set<String> QUERY_TERMS =
      ImmutableSet.of(
          SolrConfiguration.CONTENTS_ATTRIBUTE,
          SolrConfiguration.COUNTRY_CODE_ATTRIBUTE,
          SolrConfiguration.CREATED_DATE_ATTRIBUTE,
          SolrConfiguration.EXPIRATION_DATE_ATTRIBUTE,
          SolrConfiguration.ICID_ATTRIBUTE,
          SolrConfiguration.ID_ATTRIBUTE,
          SolrConfiguration.KEYWORD_ATTRIBUTE,
          SolrConfiguration.MODIFIED_ATTRIBUTE,
          SolrConfiguration.TITLE_ATTRIBUTE);

  @Bean
  public QueryStorageAdaptor queryStorageAdaptor(@NotNull final SolrClient solrClient) {
    return new SolrQueryStorageAdaptor(solrClient);
  }
}
