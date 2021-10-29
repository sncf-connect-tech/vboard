#!/bin/sh

cd /app/static
# Busybox "source" builtin only accepts full paths:
source /app/static/setup_statics.sh

cd /app
# !! A .jar must be provided, not a .war, or we will get 404s errors !!
# Also, to avoid Heroku R14 warnings, RAM could be further restrained by setting: -Xmx512m
/usr/bin/java -XX:+ExitOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -Xms256m -Xmx1g -jar vboard-ws.jar
