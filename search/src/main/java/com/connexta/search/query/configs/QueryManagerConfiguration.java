/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.configs;

import com.connexta.search.common.IndexCrudRepository;
import com.connexta.search.query.QueryManager;
import com.connexta.search.query.QueryManagerImpl;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryManagerConfiguration {

  @Bean
  public QueryManager queryManager(
      @NotNull final IndexCrudRepository indexCrudRepository,
      @NotBlank @Value("${endpointUrl.retrieve}") final String retrieveEndpoint) {
    return new QueryManagerImpl(indexCrudRepository, retrieveEndpoint);
  }
}
