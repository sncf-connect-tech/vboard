# V.Board batchs module

Batch job that synchronize the pins in an Elasticsearch index from a MySQL table.
This script is intended to be used as a cron job executed every 1 or 2 minutes.

## Properties

Batch parameters are configured in the `jdbc_sync_pins.docker.json` file.
This file uses `docker-compose` services based hostnames.
To connect to an Elasticsearch instance and a MySQL database on localhost, simply defines `VBOARD_DB_HOST=localhost` and `VBOARD_ELASTICSEARCH_HOST=localhost` before running the Docker container.
