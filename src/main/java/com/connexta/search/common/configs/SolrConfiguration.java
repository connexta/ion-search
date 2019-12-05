/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common.configs;

import java.net.MalformedURLException;
import java.net.URL;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class SolrConfiguration {

  public static final String SOLR_COLLECTION = "search_terms";

  // TODO Contents is not an attribute in the CST
  public static final String CONTENTS_ATTRIBUTE = "contents";

  // GENC:3:3-7 trigraph
  public static final String COUNTRY_CODE_ATTRIBUTE = "country_code";

  //  Date of creation of the File. Format is YYYY-MM-DDThh:mm:ss.sZ
  public static final String CREATED_DATE_ATTRIBUTE = "created";

  // Date (often a range) of validity of a File.
  public static final String EXPIRATION_DATE_ATTRIBUTE = "expiration";

  public static final String FILE_URL_ATTRIBUTE = "file_url";

  // Intelligence Community Identifier. An ICID is an unambiguous reference to the resource within a
  // given context.
  public static final String ICID_ATTRIBUTE = "icid";

  // The unique ID of a Dataset
  public static final String ID_ATTRIBUTE = "id";

  public static final String IRM_URL_ATTRIBUTE = "irm_url";

  // The topic of the File.
  public static final String KEYWORD_ATTRIBUTE = "keyword";

  public static final String METACARD_URL_ATTRIBUTE = "metacard_url";

  // Date when the file was  last changed.  Format is YYYY-MM-DDThh:mm:ss.sZ
  public static final String MODIFIED_ATTRIBUTE = "modified";

  // Name given to the resource. See Dublin Core http://purl.org/dc/terms/title
  public static final String TITLE_ATTRIBUTE = "title";

  @Bean
  @Profile("production")
  public URL solrURL(@NotBlank @Value("${endpointUrl.solr}") final String solrEndpoint)
      throws MalformedURLException {
    return new URL(solrEndpoint);
  }

  @Bean
  public SolrClient solrClient(@NotNull final URL solrUrl) {
    return new HttpSolrClient.Builder(solrUrl.toString()).build();
  }
}
