# V.Board WS module

This module is built with SpringBoot and provides a backend REST web service.

## SpringBoot profiles

- **dev**: provide properties for local development environment such as local machine or local docker install
- **test**: provide properties for automatic tests purpose

These profiles are loaded automatically by SpringBoot. They can be activated with a JVM flag: `-Dspring.profiles.active=$profile`
