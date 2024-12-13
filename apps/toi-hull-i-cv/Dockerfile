FROM gcr.io/distroless/java17
ARG APP_NAME
WORKDIR /$APP_NAME
COPY build/libs/*.jar ./
CMD ["app.jar"]
