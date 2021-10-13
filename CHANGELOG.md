# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [1.2.4] - 2021-10-13
### Fixed
- frontend `ProxyPass` configuration

## [1.2.3] - 2021-10-13
### Added
- `$DRIVER_CLASS_NAME` can now be set to define `spring.datasource.driverClassName`, which now has a default value of `com.mysql.jdbc.Driver`

## [1.2.2] - 2021-10-13
### Added
- CI: now validating that the backend image built can start without any error, using `$EXIT_AFTER_INIT`
- `$PORT` can now be defined to set the listening port
### Changed
- backend Docker image is now based on `gcr.io/distroless/java:8` instead of `tomcat`

## [1.2.1] - 2021-10-04
### Fixed
- search by label is now correctly done by the backend - closed #37
- `Variable [pinUpdate] is not defined` HTTP 500 error in `ElasticSearchClient.updatePin`
- When visiting the profile URL of a non-existing user, UI loaded forever (#44)
- per-author pin search (#9)
- issue with Cognito auth & getSessionUser, that use to rely on the `JSESSIONID` cookie
(it does not anymore)

### Changed
- renamed `$VBOARD_WP_PUBLIC_HOST` configuration environment variable into `$VBOARD_BLOG_URL`
- requiring $KCK_ENABLED to be defined when using this auth mode
(when the property was left undefined, the SpringBoot connector was misbehaving)
- properly handling "user not found" situations with a 404
- in anonymous (no-auth) mode, all users are admin
- default HTTP error is now a 500

### Added
- permalink anchor link on pins (#8)
- support for HTTPs to PinsController web crawler (#42)
- link to Swagger in web UI menu
- config option `uploads.multiplePinsPerUrlAllowed`, with corresponding environment variable `$VBOARD_ALLOW_MULTIPLE_PINS_PER_URL`
- `$ENABLE_WHITELABEL_ERRORS` optional configuration environment variable for the backend
- `$VBOARD_SUPPORT_URL` optional configuration environment variable for the frontend

### Removed
- roles `Administrateur` (redundant with `User.isAdmin`)  & `Utilisateur` (useless, correspond to any authentified user)


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
