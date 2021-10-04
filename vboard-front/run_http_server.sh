#!/bin/bash

set -o pipefail -o errexit -o nounset -o xtrace

# Variables used in httpd.conf:
: ${VBOARD_WS_HOST?'Required env variable'}
[ -z "${VBOARD_BLOG_URL:-}" ] && export VBOARD_BLOG_URL=
[ -z "${VBOARD_HOSTNAME:-}" ] && export VBOARD_HOSTNAME=localhost
[ -z "${PORT:-}" ] && export PORT=80

cd /var/www/vboard

source setup_statics.sh

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
