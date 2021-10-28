#!/usr/bin/env bash
set -o errexit -o nounset -o pipefail

# Script initializing the Elasticsearch mapping

while ! curl --fail --silent $VBOARD_ELASTICSEARCH_HOST; do
    echo "Elasticsearch endpoint $VBOARD_ELASTICSEARCH_HOST is not yet available - waiting 5s before retrying"
    sleep 5
done

echo "Uploading mapping from index_mapping.json:"
# Performing a first call with --verbose to get the error detail in case of failure:
curl --verbose --silent -XPUT $VBOARD_ELASTICSEARCH_HOST/$VBOARD_ELASTICSEARCH_INDEX/_mapping?pretty \
    -H 'Content-Type: application/json' -d @index_mapping.json
# Performing a second call with --fail to get a non-zero error code in case of failure, so that the script aborts:
curl --fail    --silent -XPUT $VBOARD_ELASTICSEARCH_HOST/$VBOARD_ELASTICSEARCH_INDEX/_mapping?pretty \
    -H 'Content-Type: application/json' -d @index_mapping.json

echo "Handing it over to /usr/local/bin/docker-entrypoint"
exec /usr/local/bin/docker-entrypoint "$@"
