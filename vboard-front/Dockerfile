FROM node:8-alpine
WORKDIR /usr/src/app
# Nécessaire pour pleeease-cli qui emploie node-gyp :
RUN apk --no-cache add --virtual native-deps g++ gcc libgcc libstdc++ linux-headers make python git autoconf automake nasm optipng zlib-dev
COPY grunt grunt
COPY install-build.sh .
COPY .bowerrc .
COPY .csslintrc .
COPY .eslintrc.json .
COPY .pleeeaserc .
COPY Gruntfile.js .
COPY bower.json .
COPY package.json .
COPY src src
RUN ./install-build.sh


FROM httpd:2.4
# Install envsubst command:
RUN apt update -y && apt install -y gettext

COPY --from=0 /usr/src/app/grunt-target  /var/www/vboard
RUN chown -R www-data:www-data           /var/www/vboard

COPY ./httpd.conf                        /usr/local/apache2/conf
COPY ./config.js                         /var/www/vboard/compile/scripts
COPY ./keycloak.json.tmpl                /var/www/vboard/compile/scripts/keycloak.json

COPY ./run_http_server.sh                /run_http_server.sh
RUN chmod u+x                            /run_http_server.sh

CMD ["/run_http_server.sh"]
