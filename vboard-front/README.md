# V.Board front module

AngularJS 1.5 app, and Docker image using Apache `httpd` to serve static content.


## Installation
This script should install everything you need, and requires `git` & `npm` in your `$PATH`:

    ./install-build.sh

## Interactive development
Execute the following command to launch a build process that will regenerate
all the static files bundles whenever a source file change is detected:

    cd vboard-front
    npm start

Note that the `bundler.js --watch` command invoked behind the scene
can somehow conflicts with `run_http_server.sh`, as this former script,
executed when the `vboard-front` container starts, also alters `config.js` & `index.html`.

Hence, currently the compromise is that `bundler.js --watch`:
- won't update `config.js`
- will override `index.html`
