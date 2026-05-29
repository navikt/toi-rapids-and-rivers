FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
ENV TZ="Europe/Oslo"
ARG APP_NAME
WORKDIR /$APP_NAME
COPY build/runtime-libs/*.jar ./runtime-libs/
COPY build/libs/app.jar ./
ENTRYPOINT ["java", "-Duser.timezone=Europe/Oslo", "-jar", "app.jar"]
