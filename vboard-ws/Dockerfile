FROM maven:3.5.4-jdk-8-alpine AS build-image
WORKDIR /usr/src/app
COPY src src
COPY pom.xml .
RUN mvn clean install -Dmaven.test.skip=true -Dfindbugs.skip=true -Dpmd.skip=true


FROM tomcat:7-jre8-alpine

RUN rm -rf /usr/local/tomcat/webapps/docs /usr/local/tomcat/webapps/examples /usr/local/tomcat/webapps/manager /usr/local/tomcat/webapps/host-manager
COPY --from=build-image /usr/src/app/target/vboard-ws-*.war  /usr/local/tomcat/webapps/vboard.war
RUN mkdir                                                    /usr/local/tomcat/properties
COPY logback.xml                                             /usr/local/tomcat/properties
COPY catalina.properties                                     /usr/local/tomcat/conf

RUN apk add --no-cache curl=~7 && rm -rf /var/cache/apk/*
HEALTHCHECK --interval=30s --timeout=30s --retries=3 CMD curl --fail http://localhost:8080/vboard/version || exit 1

# Running as non-root:
RUN chown -R nobody                                          /usr/local/tomcat
USER nobody

CMD ["catalina.sh", "run"]
