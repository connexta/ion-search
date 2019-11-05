FROM solr:8.1.1

COPY search-solr-managed-schema.xml /opt/solr/server/solr/configsets/_default/conf/managed-schema
COPY solrconfig.xml /opt/solr/server/solr/configsets/_default/conf/solrconfig.xml

ENTRYPOINT  bash -c "precreate-core search_terms;exec solr -f"