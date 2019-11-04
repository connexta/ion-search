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
import static com.connexta.search.common.configs.SolrConfiguration.EXPIRATION_DATE_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.ICID_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.ID_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.KEYWORD_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.MEDIA_TYPE_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.MODIFIED_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.QUERY_TERMS;
import static com.connexta.search.common.configs.SolrConfiguration.RESOURCE_URI_ATTRIBUTE;
import static com.connexta.search.common.configs.SolrConfiguration.TITLE_ATTRIBUTE;
import static java.util.Map.entry;

import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class SearchTestData {

  @Nullable
  public static String get(String key) {
    return map.get(key);
  }

  public static final Map<String, String> map =
      Map.ofEntries(
          entry(ID_ATTRIBUTE, "00067360b70e4acfab561fe593ad3f7b"),
          entry(CONTENTS_ATTRIBUTE, "Winterfell"),
          entry(COUNTRY_CODE_ATTRIBUTE, "USA"),
          entry(CREATED_DATE_ATTRIBUTE, "20191104T22:18:51.0Z"),
          entry(EXPIRATION_DATE_ATTRIBUTE, "20191104T22:18:51.0Z"),
          entry(ICID_ATTRIBUTE, "http://guide/123"),
          entry(KEYWORD_ATTRIBUTE, "higgs boson gravity force"),
          entry(MEDIA_TYPE_ATTRIBUTE, "application/json"),
          entry(MODIFIED_ATTRIBUTE, "20191104T22:18:51.0Z"),
          entry(RESOURCE_URI_ATTRIBUTE, "http://store/dataset/123"),
          entry(TITLE_ATTRIBUTE, "Self Guided Tour of Phnom Penh"));

  public static final String allAttributesQuery =
      QUERY_TERMS.stream()
          .map(term -> String.format("%s = '%s'", term, map.get(term)))
          .collect(Collectors.joining(" AND "));
}
