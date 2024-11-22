FROM ghcr.io/navikt/baseimages/temurin:17
ARG APP_NAME
WORKDIR /$APP_NAME
COPY build/libs/*.jar ./

EXPOSE 8301
