#!/bin/bash

set -o pipefail -o errexit -o nounset -o xtrace

: ${VBOARD_WS_HOST?'Required env variable'}
[ -z "${VBOARD_API_ENDPOINT:-}" ] && export VBOARD_API_ENDPOINT=/api/v1
[ -z "${VBOARD_BLOG_URL:-}" ] && export VBOARD_BLOG_URL=
[ -z "${VBOARD_HOSTNAME:-}" ] && export VBOARD_HOSTNAME=localhost
[ -z "${PORT:-}" ] && export PORT=80

cd /var/www/vboard

echo 'Inserting $VBOARD_API_ENDPOINT in index.html'
sed -i "s~\$VBOARD_API_ENDPOINT~${VBOARD_API_ENDPOINT:-}~" index.html

echo 'Injecting env variables in config.js:'
cp config.js{,.tmpl}
envsubst < config.js.tmpl > config.js

if [ -n "${KCK_REALM:-}" ] || [ -n "${KCK_PUBLIC_HOST:-}" ] || [ -n "${KEYCLOAK_JS_URL:-}" ]; then
    echo 'Keycloak enabled'

    if ! [ -r keycloak.json ]; then
        echo 'No keycloak.json in /var/www/vboard/'
        exit 1
    fi

    echo 'Inserting $KCK_* env variables in keycloak.json'
    sed -i "s~\$KCK_REALM~$KCK_REALM~" keycloak.json
    sed -i "s~\$KCK_PUBLIC_HOST~$KCK_PUBLIC_HOST~" keycloak.json

    echo 'Inserting $KEYCLOAK_JS_URL in index.html'
    sed -i "s~>window.Keycloak = 'DISABLED'~ src=\"/auth/js/keycloak.js\">~" index.html
fi

echo "Injecting \$PORT=$PORT in httpd.conf:"
cp /usr/local/apache2/conf/httpd.conf /usr/local/apache2/conf/httpd.conf.bak
envsubst '$PORT' < /usr/local/apache2/conf/httpd.conf.bak > /usr/local/apache2/conf/httpd.conf

HTTPD_ARGS=
echo "Launching Apache httpd in foreground: HTTP_PROXY=${HTTP_PROXY:-} KCK_PUBLIC_HOST=${KCK_PUBLIC_HOST:-}"
if [ -n "${HTTP_PROXY:-}" ]; then
    HTTPD_ARGS="${HTTPD_ARGS} -DHTTP_PROXY"
fi
if [ -n "${KCK_PUBLIC_HOST:-}" ]; then
    HTTPD_ARGS="${HTTPD_ARGS} -DKCK_PUBLIC_HOST"
fi
exec httpd -DFOREGROUND ${HTTPD_ARGS}
