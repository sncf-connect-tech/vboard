#!/bin/sh

# Script emptying the database
# Requires a Java interpreter and vboard-ws.jar

# The URL format needs to be converted in order to be JDBC-compatible:
DB_URL=$(echo $CLEARDB_DATABASE_URL | sed 's~.*//~~')
USER=$(echo $DB_URL | sed 's~:.*~~')
DB_URL=$(echo $DB_URL | sed 's~.*:~~')
PASSWORD=$(echo $DB_URL | sed 's~@.*~~')
DB_URL=$(echo $DB_URL | sed 's~.*@~~')
DB_NAME=$(echo $DB_URL | sed 's~.*/~~')
export DB_NAME=$(echo $DB_NAME | sed 's~[?].*~~')
export DB_URL="jdbc:mysql://$DB_URL&user=$USER&password=$PASSWORD"

JAR_PATH=/app/vboard-ws.jar
java -cp $JAR_PATH -Dloader.main=com.vsct.vboard.DBCleaner -Dloader.path=$JAR_PATH!/WEB-INF/classes/,$JAR_PATH!/WEB-INF/ org.springframework.boot.loader.PropertiesLauncher
# Does not work in distroless/java:8 image :(
#   Exception in thread "main" java.lang.ClassNotFoundException: com.vsct.vboard.DBCleaner
