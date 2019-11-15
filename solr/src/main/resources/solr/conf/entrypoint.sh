#!/bin/sh

init-var-solr
precreate-core search_terms

# pass all arguments on to the official solr entrypoint
exec docker-entrypoint.sh "$@"