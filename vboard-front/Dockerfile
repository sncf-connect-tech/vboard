FROM httpd:2.4

COPY ./grunt-target/           /var/www/vboard
RUN chown -R www-data:www-data /var/www/vboard

COPY ./httpd.conf              /usr/local/apache2/conf
COPY ./config.js               /var/www/vboard/compile/scripts
COPY ./keycloak.json.tmpl      /var/www/vboard/compile/scripts/keycloak.json

COPY ./run_http_server.sh      /run_http_server.sh
RUN chmod u+x                  /run_http_server.sh

CMD ["/run_http_server.sh"]
