# V.Board WS module

This module is built with SpringBoot and provides a backend REST web service.

## SpringBoot profiles

- **dev**: provide properties for local development environment such as local machine or local docker install
- **test**: provide properties for automatic tests purpose

These profiles are loaded automatically by SpringBoot. They can be activated with a JVM flag: `-Dspring.profiles.active=$profile`

If you run this app in an IDE, you will need to provide 2 properties to the JVM in order to configure where pins images are stored:
```
 -Duploads.imagesStorageDirectory=D:\path\to\vboard\statics
 -Duploads.blogImagesDirectory=D:\path\to\vboard\statics
```

## Executing in IntelliJ IDEA on Windows

First, launch the full stack:
```
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

Then stops the backend web-service container so that we can launch it with IntelliJ:
```
docker-compose -f docker-compose.yml -f docker-compose.dev.yml stop ws
```

Now configure a new SpringBoot Run/Debug Configuration in IntelliJ,
substituting `...` by the full Windows path of the repository directory :
- Main Class: `com.vsct.vboard.MainController`
- VM options: `-Duploads.imagesStorageDirectory=...\statics -Duploads.blogImagesDirectory=...\statics
- use classpath of module: `vboard-ws`
- active profile: `dev`
- Environment variables:
  * `KCK_REALM_KEY=`
  * `KCK_REALM=`
  * `VBOARD_DB_HOST=localhost:3306`

Note that currently, for lack of a better strategy, you will also have to make 2 changes to versioned files:
- change the value of the `VBOARD_API_ENDPOINT` in `docker-compose.dev.yml`,
in order for `vboard-front` to use `http://localhost:8080` as the API endpoint (and not the `vboard-ws` container one anymore)
- remove the `<scope>provided</scope>` line for `spring-boot-starter-tomcat` in `vboard-ws/pom.xml`,
because this dependency is required when executing `vboard-ws` as a standalone SpringBoot app
(whereas it is provided by Tomcat in the Docker container)
