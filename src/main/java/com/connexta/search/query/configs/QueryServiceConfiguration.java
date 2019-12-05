/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.configs;

import com.connexta.search.query.QueryService;
import com.connexta.search.query.QueryServiceImpl;
import com.connexta.search.query.QueryStorageAdaptor;
import javax.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryServiceConfiguration {

  @Bean
  public QueryService queryService(@NotNull final QueryStorageAdaptor queryStorageAdaptor) {
    return new QueryServiceImpl(queryStorageAdaptor);
  }
}
