/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.configs;

import com.connexta.search.index.IndexManager;
import com.connexta.search.index.IndexManagerImpl;
import javax.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;

@Configuration
public class IndexManagerConfiguration {

  @Bean
  public IndexManager indexManager(@NotNull final CrudRepository crudRepository) {
    return new IndexManagerImpl(crudRepository);
  }
}
