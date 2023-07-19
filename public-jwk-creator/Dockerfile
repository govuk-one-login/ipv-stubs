FROM openjdk:17-jdk-slim AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN apt-get update  && apt-get install -y unzip wget curl
RUN wget https://services.gradle.org/distributions/gradle-8.0.2-bin.zip && \
    unzip gradle-8.0.2-bin.zip && \
    ./gradle-8.0.2/bin/gradle wrapper && \
    rm -rf gradle-8.0.2*
RUN ./gradlew build --no-daemon

FROM openjdk:17-jdk-slim

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
