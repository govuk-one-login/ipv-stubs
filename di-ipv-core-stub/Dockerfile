#FROM openjdk:17-jdk-slim AS build
FROM arm64v8/openjdk:17-ea-16-jdk@sha256:149f7dbd5287cb06efc8c5d0dfffeffcc36e8a9872dca7736ef8c333a3eca6a2 AS build
WORKDIR /home/gradle/src
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
RUN ./gradlew build --no-daemon
COPY src src
RUN ./gradlew build --no-daemon

#FROM openjdk:17-jdk-slim
FROM arm64v8/openjdk:17-ea-16-jdk@sha256:149f7dbd5287cb06efc8c5d0dfffeffcc36e8a9872dca7736ef8c333a3eca6a2

ENV PORT 8085
ENV DEBUG_PORT 8087
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT
#RUN addgroup --system --gid 1001 appgroup && adduser --system --uid 1001 appuser --gid 1001
RUN mkdir /app
RUN apt-get update && apt-get install -y curl

COPY --from=build \
    /home/gradle/src/build/distributions/src.tar \
    /app/

WORKDIR /app
ADD config config

RUN tar -xvf src.tar \
    && rm src.tar

#RUN chown -R appuser:appgroup /app/
USER appuser

# Add in dynatrace layer
COPY --from=khw46367.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE $PORT
EXPOSE $DEBUG_PORT

ENTRYPOINT ["./src/bin/src"]
LABEL project="di-ipv-core-stub"
