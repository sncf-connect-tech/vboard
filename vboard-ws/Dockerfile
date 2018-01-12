FROM tomcat:7-jre8

COPY /target/vboard-ws-*.war /usr/local/tomcat/webapps/vboard.war
RUN mkdir                    /usr/local/tomcat/properties
COPY logback.xml             /usr/local/tomcat/properties
COPY catalina.properties     /usr/local/tomcat/conf

CMD ["catalina.sh", "run"]
