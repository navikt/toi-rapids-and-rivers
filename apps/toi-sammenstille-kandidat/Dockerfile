ARG BASE_IMAGE_DIGEST_PINNED_REF
FROM ${BASE_IMAGE_DIGEST_PINNED_REF}
ENV TZ="Europe/Oslo"
ARG APP_NAME
WORKDIR /$APP_NAME
COPY build/runtime-libs/*.jar ./runtime-libs/
COPY build/libs/app.jar ./

# Assume that logback.xml is located in the project/app root dir.
# The unconventional location is a signal to developers to make them aware that we use this file in an unconventional
# way in the ENTRYPOINT command in this Dockerfile.
COPY logback.xml /

# Set logback.xml explicitly and with an absolute path, to avoid accidentally using any logback.xml bundled in the JAR-files of the app's dependencies
ENTRYPOINT ["java", "-Duser.timezone=Europe/Oslo", "-Dlogback.configurationFile=/logback.xml", "-jar", "app.jar"]

EXPOSE 9000
