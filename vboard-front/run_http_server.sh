#!/bin/bash

set -o pipefail -o errexit -o nounset -o xtrace

if ! [ -d /var/www/vboard/compile ]; then
    echo 'No compile/ directory in /var/www/vboard : volume mounting probably failed'
    exit 1
fi

if [ -n "${VBOARD_API_ENDPOINT:-}" ]; then
    echo 'Inserting $VBOARD_API_ENDPOINT in index.html'
    sed -i "s~\$VBOARD_API_ENDPOINT~${VBOARD_API_ENDPOINT:-}~" /var/www/vboard/index.html
    echo 'Inserting $VBOARD_API_ENDPOINT in config.js'
    sed -i "s~\$VBOARD_API_ENDPOINT~${VBOARD_API_ENDPOINT:-}~" /var/www/vboard/compile/scripts/config.js
fi
if [ -n "${VBOARD_WP_PUBLIC_HOST:-}" ]; then
    echo 'Inserting $VBOARD_WP_PUBLIC_HOST or /vblog in config.js'
    sed -i "s~\$VBOARD_WP_PUBLIC_HOST~${VBOARD_WP_PUBLIC_HOST:-/vblog}~" /var/www/vboard/compile/scripts/config.js
fi
if [ -n "${VBOARD_LOCALISATIONS:-}" ]; then
    echo 'Inserting $VBOARD_LOCALISATIONS in config.js'
    sed -i "s~\$VBOARD_LOCALISATIONS~${VBOARD_LOCALISATIONS:-}~" /var/www/vboard/compile/scripts/config.js
fi
if [ -n "${VBOARD_PINS_MONTHS_COUNT:-}" ]; then
    echo 'Inserting $VBOARD_PINS_MONTHS_COUNT in config.js'
    sed -i "s~\$VBOARD_PINS_MONTHS_COUNT~${VBOARD_PINS_MONTHS_COUNT:-}~" /var/www/vboard/compile/scripts/config.js
fi

if [ -n "${KCK_REALM:-}" ] || [ -n "${KCK_PUBLIC_HOST:-}" ] || [ -n "${KEYCLOAK_JS_URL:-}" ]; then
    echo 'Keycloak enabled'

    if ! [ -r /var/www/vboard/compile/scripts/keycloak.json ]; then
        echo 'No keycloak.json in /var/www/vboard/compile/scripts'
        exit 1
    fi

    echo 'Inserting $KCK_* env variables in keycloak.json'
    sed -i "s/\$KCK_REALM/$KCK_REALM/" /var/www/vboard/compile/scripts/keycloak.json
    sed -i "s/\$KCK_PUBLIC_HOST/$KCK_PUBLIC_HOST/" /var/www/vboard/compile/scripts/keycloak.json

    echo 'Inserting $KEYCLOAK_JS_URL in index.html'
    sed -i "s~>window.Keycloak = 'DISABLED'~ src=\"$KEYCLOAK_JS_URL\">~" /var/www/vboard/index.html
fi

if [ -n "${EXTRA_JS_URL:-}" ]; then
    echo 'Inserting $EXTRA_JS_URL in index.html'
    sed -i "s~<!-- OPTIONAL EXTRA_JS_URL PLACEHOLDER -->~<script type=\"text/javascript\" src=\"$EXTRA_JS_URL\"></script>~" /var/www/vboard/index.html
fi

httpd-foreground
