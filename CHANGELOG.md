# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [1.1.2] - 2018-01-19
### Fixed
- Fixing `$VBOARD_WP_PUBLIC_HOST` injection in `vboard-front/run_http_server.sh`
- Upgraded some Maven dependencies to get rid of vulnerable ones - cf. https://github.com/voyages-sncf-technologies/vboard/issues/17
- Fixing bug that prevents to add a new pin - cf. https://github.com/voyages-sncf-technologies/vboard/issues/1
- Fixing typos and issues #24 #25 #26: enabling ELS dynamic scripting + duplicate entries in `docker-compose` YAMLs + missing `ng-model` in `vboardPin.html`
- Fixing pins image upload - cf. https://github.com/voyages-sncf-technologies/vboard/issues/32

## [1.1.1] - 2018-01-12
### Fixed
- Fixing `$KCK_PUBLIC_HOST` injection in `vboard-front/run_http_server.sh`

## [1.1.0] - 2018-01-12
Publication on Github
