FROM solr:8.1.1

COPY /search-solr-managed-schema.xml /opt/solr/server/solr/configsets/_default/conf/managed-schema
COPY /solrconfig.xml /opt/solr/server/solr/configsets/_default/conf/solrconfig.xml
COPY /entrypoint.sh /entrypoint.sh
USER root
RUN chmod 755 /entrypoint.sh
RUN chown -R solr:solr /var/solr

ENTRYPOINT ["/entrypoint.sh"]
CMD ["solr-foreground", "-force"]