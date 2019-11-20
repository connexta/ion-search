/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.common;

import static com.connexta.search.common.configs.SolrConfiguration.CONTENTS_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.COUNTRY_CODE_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.CREATED_DATE_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.FILE_URL_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.ID_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.IRM_URL_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.METACARD_URL_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.MODIFIED_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;
import static com.connexta.search.common.configs.SolrConfiguration.TITLE_ATTRIBUTE;

import java.util.Date;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@SolrDocument(collection = SOLR_COLLECTION)
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class Index {

  @Indexed(name = CONTENTS_ATTRIBUTE)
  private String contents;

  @Indexed(name = COUNTRY_CODE_ATTRIBUTE)
  private String countryCode;

  @Indexed(name = CREATED_DATE_ATTRIBUTE)
  private Date created;

  @Indexed(name = FILE_URL_ATTRIBUTE)
  private String fileUrl;

  @Id
  @Indexed(name = ID_ATTRIBUTE)
  private String id;

  @Indexed(name = IRM_URL_ATTRIBUTE)
  private String irmUrl;

  @Indexed(name = METACARD_URL_ATTRIBUTE)
  private String metacardUrl;

  @Indexed(name = MODIFIED_ATTRIBUTE)
  private Date modified;

  @Indexed(name = TITLE_ATTRIBUTE)
  private String title;
}
