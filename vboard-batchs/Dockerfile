FROM logstash:7.1.1
# This is the latest logstash public Docker image version to use Java 8,
# and hence the last compatible with the MySQL JDC driver below.
# (starting with 7.20, logstash Dockerfiles are available there: https://github.com/elastic/dockerfiles/blob/v7.2.0/logstash/Dockerfile )

RUN curl https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.48/mysql-connector-java-5.1.48.jar \
    -o /usr/share/logstash/tools/mysql-connector-java.jar

COPY properties/logstash.conf        /usr/share/logstash/pipeline/
COPY properties/logstash.yml         /usr/share/logstash/config/
