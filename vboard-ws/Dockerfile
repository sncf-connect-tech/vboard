FROM tomcat:7-jre8

COPY /target/vboard-ws-*.war /usr/local/tomcat/webapps/vboard.war
RUN mkdir                    /usr/local/tomcat/properties
COPY logback.xml             /usr/local/tomcat/properties
COPY catalina.properties     /usr/local/tomcat/conf

HEALTHCHECK --interval=30s --timeout=30s --retries=3 CMD curl --fail http://localhost:8080/vboard/version ||exit 1

CMD ["catalina.sh", "run"]
