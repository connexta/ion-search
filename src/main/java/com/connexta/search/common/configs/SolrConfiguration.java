/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common.configs;

import com.google.common.collect.ImmutableSet;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SolrConfiguration {

  public static final String SOLR_COLLECTION = "searchTerms";
  public static final String CONTENTS_ATTRIBUTE_NAME = "contents";
  public static final String ID_ATTRIBUTE_NAME = "id";
  public static final String LAYER_NAME = "solrLayer";
  public static final Set QUERY_TERMS = ImmutableSet.of(ID_ATTRIBUTE_NAME, CONTENTS_ATTRIBUTE_NAME);

  @Bean
  @Profile("production")
  public URL solrUrl(
      @NotBlank @Value("${solr.host}") final String host, @Value("${solr.port}") final int port)
      throws MalformedURLException {
    return new URL("http", host, port, "/solr");
  }
}
