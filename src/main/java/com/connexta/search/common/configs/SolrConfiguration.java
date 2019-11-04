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

  public static final String SOLR_COLLECTION = "search_terms";
  public static final String LAYER_NAME = "solrLayer";

  // TODO Contents is not an attribute in the CST
  public static final String CONTENTS_ATTRIBUTE = "contents";

  // The unique ID of a Dataset, internal to Ion
  public static final String ID_ATTRIBUTE = "id";

  // GENC:3:3-7 trigraph
  public static final String COUNTRY_CODE_ATTRIBUTE = "country_code";

  // TODO Make this class Date
  //  Date of creation of the File. Format is YYYY-MM-DDThh:mm:ss.sZ
  public static final String CREATED_DATE_ATTRIBUTE = "created";

  // TODO Make this class Date
  // Date (often a range) of validity of a File.
  public static final String EXPIRATION_DATE_ATTRIBUTE = "expiration";

  // Intelligence Community Identifier. An ICID is an unambiguous reference to the resource within a
  // given context.
  public static final String ICID_ATTRIBUTE = "icid";

  // The topic of the File.
  public static final String KEYWORD_ATTRIBUTE = "keyword";

  // A media type (formerly known as MIME type) is a two-part identifier for file formats and format
  // contents transmitted on the Internet.
  public static final String MEDIA_TYPE_ATTRIBUTE = "media_type";

  // TODO Make this class Date
  // Date when the file was  last changed.  Format is YYYY-MM-DDThh:mm:ss.sZ
  public static final String MODIFIED_ATTRIBUTE = "modified";

  // Location of the File
  public static final String RESOURCE_URI_ATTRIBUTE = "resource_uri";

  // Name given to the resource. See Dublin Core http://purl.org/dc/terms/title
  public static final String TITLE_ATTRIBUTE = "title";

  public static final Set<String> QUERY_TERMS =
      ImmutableSet.of(
          ID_ATTRIBUTE,
          CONTENTS_ATTRIBUTE,
          COUNTRY_CODE_ATTRIBUTE,
          CREATED_DATE_ATTRIBUTE,
          EXPIRATION_DATE_ATTRIBUTE,
          ICID_ATTRIBUTE,
          KEYWORD_ATTRIBUTE,
          MEDIA_TYPE_ATTRIBUTE,
          MODIFIED_ATTRIBUTE,
          RESOURCE_URI_ATTRIBUTE,
          TITLE_ATTRIBUTE);

  @Bean
  @Profile("production")
  public URL solrURL(@NotBlank @Value("${endpointUrl.solr}") final String solrEndpoint)
      throws MalformedURLException {
    return new URL(solrEndpoint);
  }
}
