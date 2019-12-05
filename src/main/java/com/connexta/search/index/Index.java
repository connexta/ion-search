/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.index;

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
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.Indexed;
import org.springframework.data.solr.core.mapping.SolrDocument;

/**
 * Represents a document that can be saved or retrieved from a (Solr) data {@link
 * org.springframework.data.repository.Repository}. Although this class uses Spring Data, the Solr
 * schema for the Solr core are defined in a file. Configuring properties like required, searchable,
 * and stored should not be done with Spring Data annotations. Instead, the Solr schema file should
 * be modified.
 *
 * <p>TODO Tests to make sure that id, fileUrl, metacardUrl, irmLocation are required
 */
@SolrDocument(collection = SOLR_COLLECTION)
@Builder
@Getter
@EqualsAndHashCode
@Generated
@ToString
public class Index {

  @Indexed(name = CONTENTS_ATTRIBUTE)
  @Nullable
  private final String contents;

  @Indexed(name = COUNTRY_CODE_ATTRIBUTE)
  @Nullable
  private final String countryCode;

  @Indexed(name = CREATED_DATE_ATTRIBUTE)
  @Nullable
  private final Date created;

  @Indexed(name = FILE_URL_ATTRIBUTE)
  private final String fileUrl;

  @Id
  @Indexed(name = ID_ATTRIBUTE)
  private final String id;

  @Indexed(name = IRM_URL_ATTRIBUTE)
  private final String irmUrl;

  @Indexed(name = METACARD_URL_ATTRIBUTE)
  private final String metacardUrl;

  @Nullable
  @Indexed(name = MODIFIED_ATTRIBUTE)
  private final Date modified;

  @Indexed(name = TITLE_ATTRIBUTE)
  @Nullable
  private final String title;
}
