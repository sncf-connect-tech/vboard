[![Pull Requests Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat)](http://makeapullrequest.com)
[![first-timers-only Friendly](https://img.shields.io/badge/first--timers--only-friendly-blue.svg)](http://www.firsttimersonly.com/)
-> come look at our [good first issues](https://github.com/voyages-sncf-technologies/vboard/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)

[![TravisCI build](https://travis-ci.org/voyages-sncf-technologies/vboard.svg?branch=master)](https://travis-ci.org/voyages-sncf-technologies/vboard) [![RSS from AllMyChanges](https://img.shields.io/badge/rss-allmychanges-yellow.svg)](https://allmychanges.com/rss/2b583bf388b767a02deb52966222e624/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

Dependency analysis: [![Known npm Vulnerabilities](https://snyk.io/test/github/voyages-sncf-technologies/vboard/badge.svg?targetFile=vboard-front%2Fpackage.json)](https://snyk.io/test/github/voyages-sncf-technologies/vboard?targetFile=vboard-front%2Fpackage.json)
 (npm) [![Known Maven Vulnerabilities](https://snyk.io/test/github/voyages-sncf-technologies/vboard/badge.svg?targetFile=vboard-ws%2Fpom.xml)](https://snyk.io/test/github/voyages-sncf-technologies/vboard?targetFile=vboard-ws%2Fpom.xml) (Maven)


V.Board is an information sharing app. It allows users to share "pins", that is byte-size pieces of information: an URL, a picture and a short description.
V.Board is designed to be used for communication among an organization teams: project advancement, technology watch, etc.

This software has been used at oui.sncf since July 2016.
The public, open-source version of this project was publish in January 2018. It's current status is: **INCUBATING**

- [Usage](#usage)
- [Architecture](#architecture)
  * [Docker services](#docker-services)


# Usage
The following command starts V.Board locally from the published images:

    export TAG=latest
    docker-compose -f docker-compose.yml pull
    docker-compose -f docker-compose.yml up -d --no-build

You can also rebuild the images locally:

    mvn clean install
    vboard-front/install-build.sh # requires npm
    docker-compose -f docker-compose.yml build
    docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d --no-build


# Architecture
V.Board is made of the folling modules, launched as `docker-compose` services:

- `ws`: [SpringBoot](https://projects.spring.io/spring-boot/) REST web service (backend)
- `front`: [AngularJS](https://angularjs.org) 1.5 web app served by Apache (front)
- `batchs`: Java batch job to update the ElasticSearch index

## Docker services
Some extra `docker-compose` services are used:

- `elasticsearch`: a standard Elasticsearch instance
- `wsdb`: MySQL database for the backend

There are also some volumes used by the stack:

- `images`: contains all images for vboard pins and users
It is shared between the `ws` and `front` services: `ws` writes in it and `front` reads from it.
- `wsdb-data`: contains the backend database
