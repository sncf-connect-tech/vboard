FROM maven:3.5.4-jdk-8-alpine
WORKDIR /usr/src/app
COPY src src
COPY pom.xml .
RUN mvn clean install -Dmaven.test.skip=true -Dfindbugs.skip=true -Dpmd.skip=true


FROM tomcat:7-jre8

RUN rm -rf /usr/local/tomcat/webapps/docs /usr/local/tomcat/webapps/examples /usr/local/tomcat/webapps/manager /usr/local/tomcat/webapps/host-manager
COPY --from=0 /usr/src/app/target/vboard-ws-*.war  /usr/local/tomcat/webapps/vboard.war
RUN mkdir                                          /usr/local/tomcat/properties
COPY logback.xml                                   /usr/local/tomcat/properties
COPY catalina.properties                           /usr/local/tomcat/conf

HEALTHCHECK --interval=30s --timeout=30s --retries=3 CMD curl --fail http://localhost:8080/vboard/version ||exit 1

CMD ["catalina.sh", "run"]
