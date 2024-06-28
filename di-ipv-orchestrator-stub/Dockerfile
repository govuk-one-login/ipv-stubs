FROM openjdk:17-jdk-slim AS build
COPY . /home/gradle
WORKDIR /home/gradle
RUN ./gradlew build --no-daemon --stacktrace


FROM openjdk:17-jdk-slim

ENV PORT 8083
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007
RUN addgroup --system --gid 1001 appgroup && adduser --system --uid 1001 appuser --gid 1001
RUN mkdir /app
RUN apt-get update && apt-get install -y curl
COPY --from=build \
    /home/gradle/build/distributions/di-ipv-orchestrator-stub.tar \
    /app/


## Add in jmx exporter

RUN mkdir -p /opt/jmx_exporter
COPY ./jmx_prometheus_javaagent-1.0.1.jar /opt/jmx_exporter
COPY ./build/libs/di-ipv-orchestrator-stub-all.jar /opt/jmx_exporter
COPY ./start.sh /opt/jmx_exporter
COPY ./config.yaml /opt/jmx_exporter

RUN chmod -R o+x /opt/jmx_exporter

WORKDIR /app
RUN tar -xvf di-ipv-orchestrator-stub.tar \
    && rm di-ipv-orchestrator-stub.tar

RUN chown -R appuser:appgroup /app/
USER appuser


# # Add in dynatrace layer
# COPY --from=khw46367.live.dynatrace.com/linux/oneagent-codemodules:java / /
# ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE $PORT
EXPOSE 5007
EXPOSE 9404

ENTRYPOINT exec /opt/jmx_exporter/start.sh
LABEL project="di-ipv-orchestrator-stub"
