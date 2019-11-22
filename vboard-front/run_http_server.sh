#!/bin/bash

set -o pipefail -o errexit -o nounset -o xtrace

if ! [ -d /var/www/vboard/compile ]; then
    echo 'No compile/ directory in /var/www/vboard : volume mounting probably failed'
    exit 1
fi

: ${VBOARD_API_ENDPOINT?'Required env variable'}

echo 'Inserting $VBOARD_API_ENDPOINT in index.html'
sed -i "s~\$VBOARD_API_ENDPOINT~${VBOARD_API_ENDPOINT:-}~" /var/www/vboard/index.html

echo 'Injecting env variables in config.js:'
cp /var/www/vboard/compile/scripts/config.js{,.tmpl}
envsubst < /var/www/vboard/compile/scripts/config.js.tmpl > /var/www/vboard/compile/scripts/config.js

if [ -n "${KCK_REALM:-}" ] || [ -n "${KCK_PUBLIC_HOST:-}" ] || [ -n "${KEYCLOAK_JS_URL:-}" ]; then
    echo 'Keycloak enabled'

    if ! [ -r /var/www/vboard/compile/scripts/keycloak.json ]; then
        echo 'No keycloak.json in /var/www/vboard/compile/scripts'
        exit 1
    fi

    echo 'Inserting $KCK_* env variables in keycloak.json'
    sed -i "s~\$KCK_REALM~$KCK_REALM~" /var/www/vboard/compile/scripts/keycloak.json
    sed -i "s~\$KCK_PUBLIC_HOST~$KCK_PUBLIC_HOST~" /var/www/vboard/compile/scripts/keycloak.json

    echo 'Inserting $KEYCLOAK_JS_URL in index.html'
    sed -i "s~>window.Keycloak = 'DISABLED'~ src=\"/auth/js/keycloak.js\">~" /var/www/vboard/index.html
fi

HTTPD_ARGS=
echo "Launching Apache httpd in foreground: HTTP_PROXY=${HTTP_PROXY:-} KCK_PUBLIC_HOST=${KCK_PUBLIC_HOST:-}"
if [ -n "${HTTP_PROXY:-}" ]; then
    HTTPD_ARGS="${HTTPD_ARGS} -DHTTP_PROXY"
fi
if [ -n "${KCK_PUBLIC_HOST:-}" ]; then
    HTTPD_ARGS="${HTTPD_ARGS} -DKCK_PUBLIC_HOST"
fi
exec httpd -DFOREGROUND ${HTTPD_ARGS}
