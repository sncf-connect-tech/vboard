# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [?] - 20YY-MM-DD
### Fixed
- When visiting the profile URL of a non-existing user, UI loaded forever (#44)

## [1.2.0] - 2019-11-22
### Added
- **vboard-ws**: support for AWS Cognito auth (#75)
- **vboard-front**: allowed an $HTTP_PROXY to be injected in httpd.conf

### Changed
- **vboard-ws**:
  * now compatibile with ElasticSearch 6
  * Maven build now done in Dockerfile pre-step
- **vboard-ws**: npm build now done in Dockerfile pre-step
- **vboard-batchs**: now uses logstash

### Fixed
- **vboard-front**: config parsing was broken and ignored the default values of number of last months pins to retrieve


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
