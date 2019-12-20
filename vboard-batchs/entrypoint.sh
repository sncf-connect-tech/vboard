#!/usr/bin/env bash
set -o errexit -o nounset -o pipefail

# Script responsible for initializing the Elasticsearch mapping

while ! curl --fail --silent $VBOARD_ELASTICSEARCH; do
    echo "Elasticsearch endpoint $VBOARD_ELASTICSEARCH is not yet available - waiting 5s before retrying"
    sleep 5
done

echo "Uploading mapping from jdbc_pins_index_mapping.json"
curl --fail --silent --verbose -XPUT $VBOARD_ELASTICSEARCH/jdbc_pins_index/_mapping/jdbc?pretty \
    -H 'Content-Type: application/json' -d @jdbc_pins_index_mapping.json

echo "Handing it over to /usr/local/bin/docker-entrypoint"
exec /usr/local/bin/docker-entrypoint "$@"
