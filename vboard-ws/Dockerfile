FROM maven:3.5.4-jdk-8-alpine AS builder
WORKDIR /usr/src/app
COPY src src
COPY pom.xml .
RUN mvn clean install -B -Dmaven.test.skip=true -Dfindbugs.skip=true -Dpmd.skip=true


FROM gcr.io/distroless/java:8

# Création d'un répertoire pour l'application
WORKDIR /app

# On importe le JAR de l'image intermédiaire
COPY --from=builder /usr/src/app/target/vboard-ws-*.war /app/vboard-ws.jar
COPY logback.xml                                        /app/

# On définit un user dédié à lancer l'appli:
USER 1001
# Note: en l'absence de commande "useradd" dans une image "distroless", on ajoute directement le fichier /etc/passwd
COPY passwd /etc/passwd

CMD ["-XX:+ExitOnOutOfMemoryError", "-XX:-OmitStackTraceInFastThrow", "-Xms256m", "-Xmx1g", "-jar", "vboard-ws.jar"]
# -XX:+ExitOnOutOfMemoryError : an OutOfMemoryError will often leave the JVM in an inconsistent state, so we terminate it if that happens
# -XX:-OmitStackTraceInFastThrow : avoid missing stacktraces - cf. https://plumbr.io/blog/java/on-a-quest-for-missing-stacktraces
