[ -z "${VBOARD_API_ENDPOINT:-}" ] && export VBOARD_API_ENDPOINT=/api/v1

echo "Inserting \$VBOARD_API_ENDPOINT=$VBOARD_API_ENDPOINT in index.html"
sed -i "s~\$VBOARD_API_ENDPOINT~${VBOARD_API_ENDPOINT:-}~" index.html

echo 'Injecting env variables in config.js:'
sed -i "s~\$VBOARD_API_ENDPOINT~$VBOARD_API_ENDPOINT~g" config.js
sed -i "s~\$VBOARD_BLOG_URL~$VBOARD_BLOG_URL~g" config.js
sed -i "s~\$VBOARD_SUPPORT_URL~$VBOARD_SUPPORT_URL~g" config.js
sed -i "s~\$VBOARD_LOCALISATIONS~$VBOARD_LOCALISATIONS~g" config.js
sed -i "s~\$VBOARD_PINS_MONTHS_COUNT~$VBOARD_PINS_MONTHS_COUNT~g" config.js

if [ -n "${KCK_REALM:-}" ] || [ -n "${KCK_PUBLIC_HOST:-}" ] || [ -n "${KEYCLOAK_JS_URL:-}" ]; then
    echo 'Keycloak enabled'

    if ! [ -r keycloak.json ]; then
        echo 'No keycloak.json in /var/www/vboard/'
        exit 1
    fi

    echo 'Inserting $KCK_* env variables in keycloak.json'
    sed -i "s~\$KCK_REALM~$KCK_REALM~" keycloak.json
    sed -i "s~\$KCK_PUBLIC_HOST~$KCK_PUBLIC_HOST~" keycloak.json

    echo "Inserting \$KEYCLOAK_JS_URL=$KEYCLOAK_JS_URL in index.html"
    sed -i "s~>window.Keycloak = 'DISABLED'~ src=\"/auth/js/keycloak.js\">~" index.html
fi
