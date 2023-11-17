FROM openjdk:17-jdk-slim@sha256:d732b25fa8a6944d312476805d086aeaaa6c9e2fbc3aefd482b819d5e0e32e10  AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build --no-daemon

FROM openjdk:17-jdk-slim@sha256:d732b25fa8a6944d312476805d086aeaaa6c9e2fbc3aefd482b819d5e0e32e10 

ENV PORT 8084
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006
RUN addgroup --system --gid 1001 appgroup && adduser --system --uid 1001 appuser --gid 1001
RUN mkdir /app
RUN apt-get update && apt-get install -y curl
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
