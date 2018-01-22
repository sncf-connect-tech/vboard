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
