#!/bin/sh

# Script emptying both the database & ElasticSearch index
# Requires mysql & curl CLIs

DB_URL=$(echo $CLEARDB_DATABASE_URL | sed 's~.*//~~')
USER=$(echo $DB_URL | sed 's~:.*~~')
DB_URL=$(echo $DB_URL | sed 's~.*:~~')
PASSWORD=$(echo $DB_URL | sed 's~@.*~~')
DB_URL=$(echo $DB_URL | sed 's~.*@~~')
DB_HOST=$(echo $DB_URL | sed 's~/.*~~')
DB_NAME=$(echo $DB_URL | sed 's~.*/~~')
DB_NAME=$(echo $DB_NAME | sed 's~[?].*~~')

mysql -h $DB_HOST -u $USER --password=$PASSWORD -e "DROP database $DB_NAME"
mysql -h $DB_HOST -u $USER --password=$PASSWORD -e "CREATE database $DB_NAME"

curl -sX POST $VBOARD_ELASTICSEARCH_HOST/$VBOARD_ELASTICSEARCH_INDEX/_delete_by_query -H 'Content-Type: application/json' -d '{"query": {"match_all": {}}}'
