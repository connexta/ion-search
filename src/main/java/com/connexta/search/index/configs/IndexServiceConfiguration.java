/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index.configs;

import com.connexta.search.common.IndexRepository;
import com.connexta.search.index.ContentExtractorImpl;
import com.connexta.search.index.IndexService;
import com.connexta.search.index.IndexServiceImpl;
import com.connexta.search.index.IonResourceLoader;
import javax.validation.constraints.NotNull;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class IndexServiceConfiguration {

  @Bean
  public IndexService indexService(
      @NotNull IndexRepository indexRepository, @NotNull final ResourceLoader resourceLoader) {
    // Set max document size to be 10MB characters. Assumes UTF-8 encodings are 1 byte
    return new IndexServiceImpl(
        indexRepository,
        new IonResourceLoader(resourceLoader),
        new ContentExtractorImpl(new Tika(), 10485760));
  }
}
