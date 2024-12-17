FROM gcr.io/distroless/java21
ARG APP_NAME
WORKDIR /$APP_NAME
COPY build/libs/*.jar ./
CMD ["app.jar"]
