/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.configs;

import com.connexta.search.common.SearchManager;
import com.connexta.search.index.IndexService;
import com.connexta.search.index.IndexServiceImpl;
import javax.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class IndexServiceConfiguration {

  @Bean
  public IndexService indexService(@NotNull final SearchManager searchManager) {
    return new IndexServiceImpl(searchManager, WebClient.create());
  }
}
