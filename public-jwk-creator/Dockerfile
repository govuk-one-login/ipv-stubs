FROM amazoncorretto:17-alpine3.17-jdk AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build --no-daemon

FROM amazoncorretto:17-alpine3.17-jdk

ENV PORT 8084
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006
RUN addgroup --system -g 1001 appgroup
RUN adduser --system -u 1001 appuser -G appgroup
RUN mkdir /app
RUN apk update && apk --no-cache add curl
COPY --from=build \
    /home/gradle/src/build/distributions/public-jwk-creator-1.0-SNAPSHOT.tar \
    /app/

WORKDIR /app

RUN tar -xvf public-jwk-creator-1.0-SNAPSHOT.tar \
    && rm public-jwk-creator-1.0-SNAPSHOT.tar

RUN chown -R appuser:appgroup /app/
USER appuser
EXPOSE $PORT
EXPOSE 5006

ENTRYPOINT ["./public-jwk-creator-1.0-SNAPSHOT/bin/public-jwk-creator"]
LABEL project="public-jwk-creator"
