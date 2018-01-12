#!/bin/bash

# USAGE: ./build-install.sh [$http_proxy]

set -o pipefail -o errexit -o nounset -o xtrace

cd "$( dirname "${BASH_SOURCE[0]}" )"

http_proxy=${1:-}
if [ -n "$http_proxy" ]; then
    echo "{\"directory\": \"src/main/bower_components\", \"proxy\": \"$http_proxy\", \"https-proxy\": \"$http_proxy\"}" > .bowerrc
    git config --global http.proxy $http_proxy
    npm config set proxy $http_proxy
fi

npm install -g grunt-cli@0.1.13 pleeease-cli@3.2.5
npm install

grunt validate
grunt build
