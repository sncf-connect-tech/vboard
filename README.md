[![](https://travis-ci.org/voyages-sncf-technologies/vboard.svg?branch=master)](https://travis-ci.org/voyages-sncf-technologies/vboard)

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
