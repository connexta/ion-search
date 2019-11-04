/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.configs;

import static com.connexta.search.common.configs.SolrConfiguration.SOLR_COLLECTION;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.query.QueryService;
import com.connexta.search.query.QueryServiceImpl;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.geotools.data.DataStore;
import org.geotools.data.solr.SolrAttribute;
import org.geotools.data.solr.SolrDataStore;
import org.geotools.data.solr.SolrDataStoreFactory;
import org.geotools.data.solr.SolrLayerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryServiceConfiguration {

  @Bean
  public QueryService queryService(
      @NotNull final DataStore datastore,
      @NotBlank @Value("${endpointUrl.datasetRetrieve}") final String dataRetrieveEndpoint) {
    return new QueryServiceImpl(datastore, dataRetrieveEndpoint);
  }

  @Bean
  public DataStore dataStore(@NotNull final URL solrUrl) throws IOException {
    final SolrDataStore dataStore =
        (SolrDataStore)
            new SolrDataStoreFactory()
                .createDataStore(
                    Map.of(
                        SolrDataStoreFactory.URL.key,
                        new URL(solrUrl, solrUrl.getPath() + "/" + SOLR_COLLECTION)));

    final SolrLayerConfiguration solrLayerConfiguration =
        new SolrLayerConfiguration(new ArrayList<>());
    solrLayerConfiguration.setLayerName(SolrConfiguration.LAYER_NAME);

    SolrConfiguration.QUERY_TERMS.stream()
        .forEach(
            term -> {
              SolrAttribute attribute = new SolrAttribute(term, String.class);
              attribute.setEmpty(false);
              attribute.setUse(true);
              solrLayerConfiguration.getAttributes().add(attribute);
            });

    dataStore.setSolrConfigurations(solrLayerConfiguration);
    return dataStore;
  }
}
