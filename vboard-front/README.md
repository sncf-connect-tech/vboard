# V.Board front module

AngularJS 1.5 app, and Docker image using Apache `httpd` to serve static content.


## Installation
This script should install everything you need, and requires `git` & `npm` in your `$PATH`:

    ./install-build.sh

## Interactive development
Execute the following command to launch a build process that will regenerate
all the static files bundles whenever a source file change is detected:

    cd vboard-front
    grunt watch

Note that this `grunt` task can conflicts with `run_http_server.sh` as this script,
executed when the `vboard-front` container starts, also alters `config.js` & `index.html`.

Hence, currently the compromise is that `grunt watch` by defaul:
- won't update `config.js`
- will override `index.html`
