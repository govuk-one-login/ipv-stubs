FROM openjdk:17-jdk-slim AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build --no-daemon

FROM openjdk:17-jdk-slim

ENV PORT 8084
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006
RUN addgroup --system --gid 1001 appgroup && adduser --system --uid 1001 appuser --gid 1001
RUN mkdir /app
RUN apt-get update && apt-get install -y curl
COPY --from=build \
    /home/gradle/src/build/distributions/di-ipv-credential-issuer-stub.tar \
    /app/

WORKDIR /app

RUN tar -xvf di-ipv-credential-issuer-stub.tar \
    && rm di-ipv-credential-issuer-stub.tar

RUN chown -R appuser:appgroup /app/
USER appuser
EXPOSE $PORT
EXPOSE 5006

ENTRYPOINT ["./di-ipv-credential-issuer-stub/bin/di-ipv-credential-issuer-stub"]
LABEL project="di-ipv-credential-issuer-stub"
