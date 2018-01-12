#!/bin/bash

set -o pipefail -o errexit -o nounset -o xtrace

cd "$( dirname "${BASH_SOURCE[0]}" )"

CONF_DIR=../properties
LIB_DIR=../lib/

sed -i "s/\$VBOARD_DB_HOST/$VBOARD_DB_HOST/"                       $CONF_DIR/jdbc_sync_pins.json
sed -i "s/\$VBOARD_ELASTICSEARCH_HOST/$VBOARD_ELASTICSEARCH_HOST/" $CONF_DIR/jdbc_sync_pins.json

while true; do
    echo "Synchronisation start..."
    java -cp "${LIB_DIR}/*" -Dlog4j.configurationFile=$CONF_DIR/log4j2.xml org.xbib.tools.Runner org.xbib.tools.JDBCImporter $CONF_DIR/jdbc_sync_pins.json
    echo "Synchronisation done."
    sleep 120
done
