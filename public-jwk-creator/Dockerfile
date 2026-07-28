FROM amazoncorretto:21.0.6-al2@sha256:c0643148d077b3917bf6c085aaa1ac35ff1f202344ab8691cc0f5953f9b97f8e AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN yum update -y && yum install -y findutils tar
RUN ./gradlew build --no-daemon

FROM amazoncorretto:21.0.6-al2@sha256:c0643148d077b3917bf6c085aaa1ac35ff1f202344ab8691cc0f5953f9b97f8e

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
