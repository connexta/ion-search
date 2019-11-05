/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.configs;

import com.connexta.search.index.IndexService;
import com.connexta.search.query.QueryService;
import com.connexta.search.query.QueryServiceImpl;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryServiceConfiguration {

  @Bean
  public QueryService queryService(
      @NotBlank @Value("${endpointUrl.datasetRetrieve}") final String productRetrieveEndpoint,
      @NotNull IndexService indexService) {
    return new QueryServiceImpl(productRetrieveEndpoint, indexService);
  }
}
