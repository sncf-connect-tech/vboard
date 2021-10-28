#!/bin/sh

cd /app/static
# Busybox "source" builtin only accepts full paths:
source /app/static/setup_statics.sh

cd /app
# To avoid Heroku R14 warnings, RAM could be further restrained by setting: -Xmx512m
/usr/bin/java -jar -XX:+ExitOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -Xms256m -Xmx1g -jar vboard-ws.jar
