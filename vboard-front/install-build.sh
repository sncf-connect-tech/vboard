#!/bin/sh

# USAGE: ./install-build.sh [$http_proxy]

set -o pipefail -o errexit -o nounset -o xtrace

cd "$( dirname "$0" )"

http_proxy=${1:-}
if [ -n "$http_proxy" ]; then
    git config --global http.proxy $http_proxy
    npm config set proxy $http_proxy
fi

npm install
npm run build
