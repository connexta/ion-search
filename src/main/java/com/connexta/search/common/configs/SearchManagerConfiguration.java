/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common.configs;

import com.connexta.search.common.IndexRepository;
import com.connexta.search.common.SearchManager;
import com.connexta.search.common.SearchManagerImpl;
import java.net.URL;
import javax.validation.constraints.NotNull;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@EnableSolrRepositories(basePackages = "com.connexta.search.common")
@Configuration
public class SearchManagerConfiguration {

  @Bean
  public SolrClient solrClient(@NotNull final URL solrUrl) {
    return new HttpSolrClient.Builder(solrUrl.toString()).build();
  }

  @Bean
  public SolrTemplate solrTemplate(@NotNull final SolrClient client) {
    return new SolrTemplate(client);
  }

  @Bean
  public SearchManager searchManager(
      @NotNull final IndexRepository indexRepository, @NotNull final SolrClient solrClient) {
    return new SearchManagerImpl(indexRepository, solrClient);
  }
}
