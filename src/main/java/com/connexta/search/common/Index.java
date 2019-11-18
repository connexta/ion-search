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
import static com.connexta.search.common.configs.SolrConfiguration.ID_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.IRM_URI_STRING_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

@SolrDocument(collection = SOLR_COLLECTION)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class Index {

  @Id
  @Indexed(name = ID_ATTRIBUTE, type = "string")
  private String id;

  @Indexed(name = CONTENTS_ATTRIBUTE, type = "string")
  private String contents;

  @Indexed(name = IRM_URI_STRING_ATTRIBUTE, type = "string")
  private String irmUriString;

  @Indexed(name = COUNTRY_CODE_ATTRIBUTE, type = "string")
  private String countryCode;

  @Indexed(name = CREATED_DATE_ATTRIBUTE, type = "date")
  private Date created;

  private Date modified;

  private String icid;
}
