FROM eclipse-temurin:21-jre-alpine
LABEL authors="johny"

#uid/gid ftpuser-a
ARG APP_UID=995
ARG APP_GID=987

# Создаем группу и пользователя
RUN addgroup -g ${APP_GID} app && adduser -D -G app -u ${APP_UID} app

WORKDIR /app

COPY target/ftp-robot-1.0.jar /app/app.jar

RUN mkdir -p /app/logs && chown -R app:app /app

USER app

VOLUME ["/srv/ftp", "/var/log/ftp-robot"]

ENTRYPOINT ["java","-jar","/app/app.jar"]