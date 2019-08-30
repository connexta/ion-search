/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.search.query.configs;

import com.connexta.search.common.configs.SolrConfiguration;
import com.connexta.search.query.QueryManager;
import com.connexta.search.query.QueryManagerImpl;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
public class QueryManagerConfiguration {

  @Bean
  public QueryManager queryManager(
      @NotNull final DataStore datastore,
      @NotBlank @Value("${endpointUrl.retrieve}") final String retrieveEndpoint) {
    return new QueryManagerImpl(datastore, retrieveEndpoint);
  }

  @Bean
  public DataStore dataStore(@NotNull final URL solrUrl) throws IOException {
    final SolrDataStore dataStore =
        (SolrDataStore)
            new SolrDataStoreFactory()
                .createDataStore(Map.of(SolrDataStoreFactory.URL.key, solrUrl + "/searchTerms"));

    final SolrLayerConfiguration solrLayerConfiguration =
        new SolrLayerConfiguration(new ArrayList<>());
    solrLayerConfiguration.setLayerName(SolrConfiguration.LAYER_NAME);
    final List<SolrAttribute> layerAttributes = new ArrayList<>();

    // SolrAttribute [name=contents, type=class java.lang.String, pk=false, use=false,
    // multivalued=false, empty=false, srid=null, defaultGeometry=false]"
    final SolrAttribute contentsSolrAttribute =
        new SolrAttribute(SolrConfiguration.CONTENTS_ATTRIBUTE_NAME, String.class);
    contentsSolrAttribute.setEmpty(false);
    contentsSolrAttribute.setUse(true);
    layerAttributes.add(contentsSolrAttribute);

    // SolrAttribute [name=id, type=class java.lang.String, pk=true, use=true, multivalued=false,
    // empty=false, srid=null, defaultGeometry=false]"
    final SolrAttribute idSolrAttribute =
        new SolrAttribute(SolrConfiguration.ID_ATTRIBUTE_NAME, String.class);
    idSolrAttribute.setEmpty(false);
    idSolrAttribute.setUse(true);
    layerAttributes.add(idSolrAttribute);

    solrLayerConfiguration.getAttributes().addAll(layerAttributes);
    dataStore.setSolrConfigurations(solrLayerConfiguration);
    return dataStore;
  }
}
