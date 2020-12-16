FROM node:8-alpine AS build-image
WORKDIR /usr/src/app
COPY .pleeeaserc .
COPY bundled.json .
COPY bundler.js .
COPY install-build.sh .
COPY package.json .
COPY src src
RUN ./install-build.sh


FROM httpd:2.4-alpine

RUN apk add --no-cache bash=~5 gettext=~0.20 libcap=~2 && rm -rf /var/cache/apk/*

COPY --from=build-image /usr/src/app/build  /var/www/vboard
RUN chmod a+x                               /var/www/vboard/images

COPY ./config.js                            /var/www/vboard/
COPY ./keycloak.json.tmpl                   /var/www/vboard/keycloak.json
COPY ./httpd.conf                           /usr/local/apache2/conf

COPY ./run_http_server.sh                   /run_http_server.sh
RUN chmod u+x                               /run_http_server.sh

# Change access righs to conf, logs, bin from root to www-data
RUN chown -hR www-data:www-data /usr/local/apache2/ /var/www/vboard/ /run_http_server.sh

# setcap to bind to privileged ports as non-root
RUN setcap 'cap_net_bind_service=+ep' /usr/local/apache2/bin/httpd
RUN getcap /usr/local/apache2/bin/httpd

# Using a non-root user to launch httpd:
USER www-data

CMD ["/run_http_server.sh"]
