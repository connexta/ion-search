/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.configs;

import com.connexta.search.index.IndexRepository;
import com.connexta.search.index.IndexService;
import com.connexta.search.index.IndexServiceImpl;
import java.net.URL;
import javax.validation.constraints.NotNull;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@EnableSolrRepositories(basePackages = "com.connexta.search.index")
@Configuration
public class IndexServiceConfiguration {

  @Bean
  public SolrClient solrClient(@NotNull final URL solrUrl) {
    return new HttpSolrClient.Builder(solrUrl.toString()).build();
  }

  @Bean
  public SolrTemplate solrTemplate(@NotNull final SolrClient client) {
    return new SolrTemplate(client);
  }

  @Bean
  public IndexService indexService(
      @NotNull final IndexRepository indexRepository, @NotNull final SolrClient solrClient) {
    return new IndexServiceImpl(indexRepository, solrClient);
  }
}
