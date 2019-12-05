FROM node:8-alpine
WORKDIR /usr/src/app
COPY .pleeeaserc .
COPY bundled.json .
COPY bundler.js .
COPY install-build.sh .
COPY package.json .
COPY src src
RUN ./install-build.sh


FROM httpd:2.4
# Install envsubst command:
RUN apt update -y && apt install -y gettext

COPY --from=0 /usr/src/app/build  /var/www/vboard
RUN chown -R www-data:www-data    /var/www/vboard
RUN chmod a+x                     /var/www/vboard/images

COPY ./config.js                  /var/www/vboard/
COPY ./keycloak.json.tmpl         /var/www/vboard/keycloak.json
COPY ./httpd.conf                 /usr/local/apache2/conf

COPY ./run_http_server.sh         /run_http_server.sh
RUN chmod u+x                     /run_http_server.sh

CMD ["/run_http_server.sh"]
