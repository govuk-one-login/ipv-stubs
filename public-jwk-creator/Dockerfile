FROM amazoncorretto:17 AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build --no-daemon

FROM amazoncorretto:17

ENV PORT=8084
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006"
RUN yum update -y && yum install -y shadow-utils curl tar
RUN groupadd -r -g 1001 appgroup && useradd -r -u 1001 -g appgroup appuser
RUN mkdir /app
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
